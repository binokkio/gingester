package b.nana.technology.gingester.transformers.claude;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.annotations.Experimental;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
public final class GcliHelper implements Transformer<Object, String> {

    private static final Logger logger = LoggerFactory.getLogger(GcliHelper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final String apiKey;
    private final String anthropicVersion;
    private final String model;
    private final int maxTokens;
    private final ArrayNode tools;
    private final ArrayNode system;
    private final Template userPromptOverride;
    private final Template gcliPrelude;
    private final Template gcliEditable;

    public GcliHelper(Parameters parameters) {
        apiKey = requireNonNull(parameters.apiKey);
        anthropicVersion = requireNonNull(parameters.anthropicVersion);
        model = requireNonNull(parameters.model);
        maxTokens = parameters.maxTokens;
        userPromptOverride = parameters.userPromptOverride == null ? null : Context.newTemplate(parameters.userPromptOverride);
        gcliPrelude = parameters.gcliPrelude == null ? null : Context.newTemplate(parameters.gcliPrelude);
        gcliEditable = parameters.gcliEditable == null ? null : Context.newTemplate(parameters.gcliEditable);

        try {

            tools = (ArrayNode) objectMapper.readTree(TOOLS);

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

    @Override
    public void transform(Context context, Object in, Receiver<String> out) throws Exception {

        String gcliPrelude = this.gcliPrelude != null ? this.gcliPrelude.render(context, in) : null;
        StringBuilder gcli = new StringBuilder();

        ArrayNode messages = (ArrayNode) objectMapper.readTree("[{\"role\": \"user\", \"content\": [{ \"type\": \"text\" }]}]");
        ((ObjectNode) messages.get(0).get("content").get(0)).put("text", userPromptOverride != null ? userPromptOverride.render(context, in) : DEFAULT_USER_PROMPT);

        if (gcliEditable != null) {
            gcli.append(gcliEditable.render(context, in));
            messages.add(objectMapper.readTree("{\"role\": \"assistant\", \"content\": [{ \"type\": \"tool_use\", \"id\": \"1\", \"name\": \"str_replace_editor\", \"input\": {\"command\": \"view\", \"path\": \"/main.gcli\" }}]}"));
            messages.add(createToolResultMessage("1", gcli.toString()));
        }

        ObjectNode prompt = JsonNodeFactory.instance.objectNode();
        prompt.put("model", model);
        prompt.put("max_tokens", maxTokens);
        prompt.set("tools", tools);
        prompt.set("system", system);
        prompt.set("messages", messages);

        boolean loop;
        do {
            loop = false;

            HttpRequest.Builder requestBuilder = HttpRequest
                    .newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", anthropicVersion)
                    .header("content-type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(prompt)));

            HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
            JsonNode json = objectMapper.readTree(response.body());
            logger.info("Claude: {}", json.toPrettyString());

            if (json.get("type").asText().equals("error"))
                throw new IllegalStateException("Received error response");

            messages.add(prune(json));

            for (JsonNode content : json.get("content")) {
                if (content.get("type").asText().equals("tool_use")) {
                    loop = true;
                    String toolUseId = content.get("id").asText();
                    JsonNode input = content.get("input");
                    switch (content.get("name").asText()) {
                        case "str_replace_editor": handleStrReplaceEditorUse(toolUseId, input, gcli, messages); break;
                        case "gingester": handleGingesterUse(toolUseId, input, gcliPrelude, gcli, messages); break;
                        default: throw new UnsupportedOperationException("Unsupported tool: " + content.get("name"));
                    }
                }
            }

        } while (loop);

        out.accept(context, gcli.toString());
    }

    private JsonNode prune(JsonNode response) {
        ObjectNode pruned = JsonNodeFactory.instance.objectNode();
        pruned.set("role", response.get("role"));
        pruned.set("content", response.get("content"));
        return pruned;
    }

    private void handleStrReplaceEditorUse(String toolUseId, JsonNode input, StringBuilder gcli, ArrayNode messages) throws JsonProcessingException {
        switch (input.get("command").asText()) {

            case "create":
                gcli.setLength(0);
                gcli.append(input.get("file_text").asText());
                messages.add(createToolResultMessage(toolUseId, gcli.toString()));
                break;

            case "str_replace":
                String oldStr = input.get("old_str").asText();
                int indexOf = gcli.indexOf(oldStr);
                if (indexOf == -1) throw new IllegalStateException("No occurrences of old_str");
                int lastIndexOf = gcli.lastIndexOf(oldStr);
                if (indexOf != lastIndexOf) throw new IllegalStateException("Multiple occurrences of old_str");
                String newStr = input.get("new_str").asText();
                gcli.replace(indexOf, indexOf + oldStr.length(), newStr);
                messages.add(createToolResultMessage(toolUseId, gcli.toString()));
                break;

            default: throw new UnsupportedOperationException("Unsupported command: " + input.get("command"));
        }

        logger.info("GCLI: {}", gcli);
    }

    private void handleGingesterUse(String toolUseId, JsonNode input, String gcliPrelude, StringBuilder gcli, ArrayNode messages) throws JsonProcessingException {

        String[] result = new String[1];
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        try {
            FlowBuilder flowBuilder = new FlowBuilder().seedValue(errorStream).cli("-s errorStream -e errorStream");
            if (gcliPrelude != null) flowBuilder.cli(gcliPrelude);
            if (!input.get("transformer").asText().equals("__seed__")) {
                if (input.has("kwargs")) flowBuilder.cli(gcli.toString(), input.get("kwargs"));
                else flowBuilder.cli(gcli.toString());
                if (input.has("context")) flowBuilder.seedStash(objectMapper.treeToValue(input.get("context"), new TypeReference<Map<String, Object>>(){}));
                if (input.get("command").asText().startsWith("get_input")) flowBuilder.knife(input.get("transformer").asText());
                else if (input.get("command").asText().startsWith("get_output")) flowBuilder.divert(input.get("transformer").asText());
                else throw new UnsupportedOperationException("Unsupported command: " + input.get("command"));
            }
            flowBuilder.cli("-a String -t Head 1 interrupt").add((context, value) -> {
                    switch (input.get("command").asText()) {
                        case "get_input_value":
                        case "get_output_value":
                            result[0] = (String) value;
                            break;
                        case "get_input_context":
                        case "get_output_context":
                            result[0] = context.prettyStash(999);
                            break;
                        default: throw new UnsupportedOperationException("Unsupported command: " + input.get("command"));
                    }
                }).run();
        } catch (Exception e) {
            PrintStream printStream = new PrintStream(errorStream, false);
            printStream.println(e.getMessage());
            e.printStackTrace(printStream);
        }

        if (result[0] != null) messages.add(createToolResultMessage(toolUseId, result[0]));
        else if (errorStream.size() > 0) messages.add(createToolResultMessage(toolUseId, errorStream.toString()));
        else messages.add(createToolResultMessage(toolUseId, "Transformer " + input.get("transformer") + "did not produce output"));
    }

    private JsonNode createToolResultMessage(String toolUseId, String content) throws JsonProcessingException {
        JsonNode message = objectMapper.readTree("{\"role\": \"user\", \"content\": [{ \"type\": \"tool_result\", \"tool_use_id\": \"1\" }]}");
        ((ObjectNode) message.get("content").get(0)).put("tool_use_id", toolUseId);
        ((ObjectNode) message.get("content").get(0)).put("content", content);
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
        public TemplateParameters userPromptOverride;
        public TemplateParameters gcliPrelude;
        public TemplateParameters gcliEditable;
    }

    private static final String TOOLS = """
            [{
                "type": "text_editor_20250124",
                "name": "str_replace_editor"
            }, {
                "name": "gingester",
                "description": "Get an example of the output for the given transformer if \\"/main.gcli\\" was run with the given kwargs and context.",
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
            }]
            """;

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are an AI assistant tasked with writing and improving Gingester GCLI code in the file \"/main.gcli\". Don't use pure transformers unless necessary. Don't write GCLI for anything you are not explicitly asked for. Don't add error handling unless explicitly asked for. Keep it minimal. Get user input from kwargs, e.g. [=foo] to get the value of the `foo` kwarg. Build the GCLI in small steps, add a few transformers at most each step. Don't guess the structure of transformer outputs, use the \"get_output\" tool to get the output of the last transformer to figure out the next step (make sure to give it an id in GCLI).";

    private static final String DEFAULT_USER_PROMPT =
            "Please help me write GCLI to look up the capital of a country using https://restcountries.com/v3.1/name/{countryName}";
}
