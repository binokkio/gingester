package b.nana.technology.gingester.transformers.claude;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.annotations.Experimental;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.Held;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import b.nana.technology.gingester.core.transformers.stash.StaticStash;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Experimental
public final class GcliHelper implements Transformer<Object, ArrayNode> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final String apiKey;
    private final String anthropicVersion;
    private final String model;
    private final int maxTokens;
    private final ArrayNode tools;
    private final ArrayNode system;
    private final Template gcliPrelude;
    private final Template gcliEditable;
    private final boolean interactive;
    private final Map<String, String> mask;

    private Held held;

    public GcliHelper(Parameters parameters) {
        apiKey = requireNonNull(parameters.apiKey);
        anthropicVersion = requireNonNull(parameters.anthropicVersion);
        model = requireNonNull(parameters.model);
        maxTokens = parameters.maxTokens;
        gcliPrelude = parameters.gcliPrelude == null ? null : Context.newTemplate(parameters.gcliPrelude);
        gcliEditable = parameters.gcliEditable == null ? null : Context.newTemplate(parameters.gcliEditable);
        interactive = parameters.interactive;
        mask = parameters.mask;

        try {

            // prepare tools
            tools = JsonNodeFactory.instance.arrayNode();
            tools.add(objectMapper.readTree(getEditorTool(model)));
            tools.add(objectMapper.readTree(GINGESTER_TOOL));

            // prepare system prompt
            system = (ArrayNode) objectMapper.readTree("[{\"type\": \"text\"}]");
            ((ObjectNode) system.get(0)).put("text", parameters.systemPromptOverride != null ? parameters.systemPromptOverride : DEFAULT_SYSTEM_PROMPT);

            // add usage guide
            new FlowBuilder()
                    .cli("-t JsonDef @ '{type:\"text\"}' -s -t ResourceOpen /gingester/core/help.txt -a String -t StringToJsonNode -t JsonSet text")
                    .add(s -> system.insert(0, (JsonNode) s))
                    .run();

            // add transformers
            TransformerFactory transformerFactory = parameters.providers != null && !parameters.providers.isEmpty() ?
                    TransformerFactory.withProvidersByFqdn(parameters.providers) :
                    TransformerFactory.withSpiProviders();

            new FlowBuilder()
                    .seedValue(transformerFactory.getTransformerHelps().collect(Collectors.joining("\n")))
                    .cli("-s transformers -t JsonDef @ '{type:\"text\"}' -s -f transformers -t InputStreamPrepend 'Available transformers:${\"\n\"}' -a String -t StringToJsonNode -t JsonSet text")
                    .add(s -> system.insert(1, (JsonNode) s))
                    .run();

            // add examples
            new FlowBuilder()
                    .seedValue(parameters.examplesPath)
                    .cli("-t PathSearch '{root:\"${__in__}\",globs:\"**\"}' -a String -ss example '<document>\n\t<source>${description}</source>\n\t<document_content>\n${__in__}\n\t</document_content>\n</document>' -t JsonDef @ '{type:\"text\"}' -s -f example -t StringToJsonNode -t JsonSet text")
                    .add(s -> system.insert(2, (JsonNode) s))
                    .run();

            // add cache control on the last system prompt entry
            objectMapper.readerForUpdating(system.get(system.size() - 1)).readTree("{\"cache_control\": {\"type\": \"ephemeral\"}}");

        } catch (IOException e) {
            throw new RuntimeException(e);  // TODO
        }
    }

    private String getEditorTool(String model) {
        if (model.contains("-4-")) return CLAUDE_4_0_TEXT_EDITOR_TOOL;
        else if (model.contains("-3-7-")) return CLAUDE_3_7_TEXT_EDITOR_TOOL;
        else if (model.contains("-3-5-")) return CLAUDE_3_5_TEXT_EDITOR_TOOL;
        else throw new IllegalArgumentException("Unknown model: " + model);
    }

    @Override
    public void setup(SetupControls controls) {
        this.held = controls.getHeld();
    }

    @Override
    public void transform(Context context, Object in, Receiver<ArrayNode> out) throws Exception {

        ArrayNode messages = JsonNodeFactory.instance.arrayNode();
        if (in instanceof String string) messages.add(string);
        else if (in instanceof TextNode textNode) messages.add(textNode);
        else if (in instanceof ArrayNode arrayNode) messages = arrayNode;
        else throw new IllegalArgumentException("Unexpected input: " + in);

        for (int i = 0; i < messages.size(); i++) {

            JsonNode message = messages.get(i);

            if (message.isTextual()) {
                message = upgrade(message);
                messages.set(i, message);
            }

            for (JsonNode content : message.path("content")) {
                if (content.path("type").asText().equals("tool_use")) {
                    if (i != messages.size() - 1 && !messages.get(i + 1).path("content").path(0).path("type").asText().equals("tool_result")) {
                        messages.insert(i + 1, createToolResultMessage(
                                content.get("id").asText(),
                                "Error: Tool use cancelled by user.",
                                true
                        ));
                    }
                }
            }
        }

        String gcliPrelude = this.gcliPrelude != null ? this.gcliPrelude.render(context) : null;
        StringBuilder gcli = new StringBuilder();
        if (gcliEditable != null) {
            gcli.append(gcliEditable.render(context));
            if (messages.size() == 1) {
                messages.add(objectMapper.readTree("{\"role\": \"assistant\", \"content\": [{ \"type\": \"tool_use\", \"id\": \"1\", \"name\": \"" + tools.get(0).get("name").asText() + "\", \"input\": {\"command\": \"view\", \"path\": \"/main.gcli\" }}]}"));
                messages.add(createToolResultMessage("1", gcli.toString(), false));
            }
        }

        boolean loop;
        do {
            loop = false;

            JsonNode lastMessage = messages.get(messages.size() - 1);

            switch (lastMessage.get("role").asText()) {

                case "user":

                    ObjectNode prompt = JsonNodeFactory.instance.objectNode();
                    prompt.put("model", model);
                    prompt.put("max_tokens", maxTokens);
                    prompt.set("tools", tools);
                    prompt.set("system", system);
                    prompt.set("messages", messages);

                    HttpRequest.Builder requestBuilder = HttpRequest
                            .newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                            .header("x-api-key", apiKey)
                            .header("anthropic-version", anthropicVersion)
                            .header("content-type", "application/json")
                            .method("POST", HttpRequest.BodyPublishers.ofString(mask(objectMapper.writeValueAsString(prompt))));

                    HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                    JsonNode json = objectMapper.readTree(unmask(response.body()));

                    if (json.get("type").asText().equals("error")) {
                        messages.add(json);
                    } else {
                        messages.add(prune(json));
                        loop = !interactive;
                    }

                    break;

                case "assistant":

                    for (JsonNode content : lastMessage.get("content")) {
                        if (content.get("type").asText().equals("tool_use")) {
                            loop = true;
                            String toolUseId = content.get("id").asText();
                            JsonNode input = content.get("input");
                            switch (content.get("name").asText()) {
                                case "str_replace_editor":
                                case "str_replace_based_edit_tool": messages.add(handleEditorUse(toolUseId, input, gcli)); break;
                                case "gingester": messages.add(handleGingesterUse(toolUseId, input, gcliPrelude, gcli)); break;
                                default: throw new UnsupportedOperationException("Unsupported tool: " + content.get("name"));
                            }
                        }
                    }

                    break;

                default: throw new UnsupportedOperationException("Unknown role: " + lastMessage.get("role"));
            }

        } while (loop);

        out.accept(context, messages);
    }

    private JsonNode upgrade(JsonNode jsonNode) throws JsonProcessingException {
        JsonNode result = objectMapper.readTree("{\"role\": \"user\", \"content\": [{ \"type\": \"text\" }]}");
        ((ObjectNode) result.get("content").get(0)).set("text", jsonNode);
        return result;
    }

    private String mask(String string) {
        if (mask == null) return string;
        String[] holder = new String[] { string };
        mask.forEach((replace, with) -> holder[0] = holder[0].replaceAll(replace, with));
        return holder[0];
    }

    private String unmask(String string) {
        if (mask == null) return string;
        String[] holder = new String[] { string };
        mask.forEach((with, replace) -> holder[0] = holder[0].replaceAll(replace, with));
        return holder[0];
    }

    private JsonNode prune(JsonNode response) {
        ObjectNode pruned = JsonNodeFactory.instance.objectNode();
        pruned.set("role", response.get("role"));
        pruned.set("content", response.get("content"));
        return pruned;
    }

    private JsonNode handleEditorUse(String toolUseId, JsonNode input, StringBuilder gcli) throws JsonProcessingException {
        switch (input.get("command").asText()) {

            case "view":
                return createToolResultMessage(toolUseId, gcli.toString(), false);

            case "create":
                gcli.setLength(0);
                gcli.append(input.get("file_text").asText());
                return createToolResultMessage(toolUseId, gcli.toString(), false);

            case "str_replace":
                String oldStr = input.get("old_str").asText();
                int indexOf = gcli.indexOf(oldStr);
                if (indexOf == -1) return createToolResultMessage(toolUseId, "Error: No match found for replacement. Please check your text and try again.", true);
                int lastIndexOf = gcli.lastIndexOf(oldStr);
                if (indexOf != lastIndexOf) return createToolResultMessage(toolUseId, "Error: Found multiple matches for replacement text. Please provide more context to make a unique match.", true);
                String newStr = input.get("new_str").asText();
                gcli.replace(indexOf, indexOf + oldStr.length(), newStr);
                return createToolResultMessage(toolUseId, gcli.toString(), false);

            default: throw new UnsupportedOperationException("Unsupported command: " + input.get("command"));
        }
    }

    private JsonNode handleGingesterUse(String toolUseId, JsonNode input, String gcliPrelude, StringBuilder gcli) throws JsonProcessingException {

        String[] result = new String[1];
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        try {
            FlowBuilder flowBuilder = new FlowBuilder(held).seedStash(Map.of("errorStream", errorStream)).cli("-e errorStream");
            if (gcliPrelude != null) flowBuilder.cli(gcliPrelude);
            if (!input.get("transformer").asText().equals("__seed__")) {
                if (input.path("context").isObject()) flowBuilder.add(new StaticStash(objectMapper.treeToValue(input.get("context"), new TypeReference<>() {})));
                if (input.path("kwargs").isObject()) flowBuilder.cli(gcli.toString(), input.get("kwargs"));
                else flowBuilder.cli(gcli.toString());
                if (input.get("command").asText().startsWith("get_input")) flowBuilder.knife(input.get("transformer").asText());
                else if (input.get("command").asText().startsWith("get_output")) flowBuilder.divert(input.get("transformer").asText());
                else throw new UnsupportedOperationException("Unsupported command: " + input.get("command"));
            }
            switch (input.get("command").asText()) {
                case "get_input_value":
                case "get_output_value":
                    flowBuilder.cli("-a String -t Head 1 interrupt").add((context, value) -> result[0] = (String) value).run();
                    break;
                case "get_input_context":
                case "get_output_context":
                    flowBuilder.cli("-t Head 1 interrupt").add((context, value) -> result[0] = context.prettyStash(999)).run();
                    break;
                default: throw new UnsupportedOperationException("Unsupported command: " + input.get("command"));
            }
        } catch (Exception e) {
            PrintStream printStream = new PrintStream(errorStream, false);
            e.printStackTrace(printStream);
            printStream.flush();
        }

        if (result[0] != null) return createToolResultMessage(toolUseId, result[0], false);
        else if (errorStream.size() > 0) return createToolResultMessage(toolUseId, ErrorStringPruner.prune(errorStream.toString()), true);
        else return createToolResultMessage(toolUseId, "Transformer " + input.get("transformer") + " did not " + (input.get("command").asText().startsWith("get_input") ? "receive input" : "produce output"), true);
    }

    private JsonNode createToolResultMessage(String toolUseId, String content, boolean isError) throws JsonProcessingException {
        JsonNode message = objectMapper.readTree("{\"role\": \"user\", \"content\": [{ \"type\": \"tool_result\" }]}");
        ((ObjectNode) message.get("content").get(0)).put("tool_use_id", toolUseId);
        ((ObjectNode) message.get("content").get(0)).put("content", content);
        if (isError) ((ObjectNode) message.get("content").get(0)).put("is_error", true);
        return message;
    }

    public static class Parameters {
        public String apiKey;
        public String anthropicVersion = "2023-06-01";
        public String model = "claude-3-7-sonnet-20250219";
        public int maxTokens = 4096;
        public List<String> providers;
        public String examplesPath;
        public String systemPromptOverride;
        public TemplateParameters gcliPrelude;
        public TemplateParameters gcliEditable;
        public boolean interactive;
        public Map<String, String> mask;
    }

    private static final String CLAUDE_3_5_TEXT_EDITOR_TOOL = """
            {
                "type": "text_editor_20241022",
                "name": "str_replace_editor"
            }
            """;

    private static final String CLAUDE_3_7_TEXT_EDITOR_TOOL = """
            {
                "type": "text_editor_20250124",
                "name": "str_replace_editor"
            }
            """;

    private static final String CLAUDE_4_0_TEXT_EDITOR_TOOL = """
            {
                "type": "text_editor_20250429",
                    "name": "str_replace_based_edit_tool"
            }
            """;

    private static final String GINGESTER_TOOL = """
            {
                "name": "gingester",
                "description": "Get an example of the input or output for the given transformer if \\"/main.gcli\\" was run with the given kwargs and context.",
                "input_schema": {
                    "type": "object",
                    "properties": {
                        "command": {
                            "type": "string",
                            "enum": ["get_input_context", "get_input_value", "get_output_context", "get_output_value"]
                        },
                        "transformer": {
                            "type": "string",
                            "description": "The id of the transformer or __seed__."
                        },
                        "kwargs": {
                            "type": "object",
                            "additionalProperties": true
                        },
                        "context": {
                            "type": "object",
                            "additionalProperties": true
                        }
                    },
                    "required": ["command", "transformer"]
                }
            }
            """;

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are an AI assistant tasked with writing and improving Gingester GCLI code in the file \"/main.gcli\". Don't use pure transformers unless necessary. Don't write GCLI for anything you are not explicitly asked for. Don't handle edge cases unless explicitly asked for. Don't add error handling unless explicitly asked for. Keep it minimal. Get user input from kwargs, e.g. [=foo] to get the value of the `foo` kwarg. Build the GCLI in small steps, add a few transformers at most each step. Don't guess the structure of transformer outputs, use the \"get_output\" tool to get the output of the last transformer to figure out the next step (make sure to give it an id in GCLI).";
}
