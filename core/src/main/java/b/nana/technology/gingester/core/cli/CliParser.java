package b.nana.technology.gingester.core.cli;

import b.nana.technology.gingester.core.configuration.GingesterConfiguration;
import b.nana.technology.gingester.core.configuration.TransformerConfiguration;
import b.nana.technology.gingester.core.template.FreemarkerTemplateFactory;
import b.nana.technology.gingester.core.template.FreemarkerTemplateWrapper;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static b.nana.technology.gingester.core.template.FreemarkerTemplateFactory.createCliTemplate;
import static java.util.Objects.requireNonNull;

public final class CliParser {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    static final ObjectReader OBJECT_READER = OBJECT_MAPPER.reader();

    private CliParser() {}

    public static GingesterConfiguration parse(String template, Object parameters) {
        String cli = FreemarkerTemplateFactory.createCliTemplate("string", template).render(parameters);
        return parse(CliSplitter.split(cli));
    }

    public static GingesterConfiguration parse(URL template, Object parameters) {
        try {
            String templateName = template.toString();
            String templateSource = new String(template.openStream().readAllBytes(), StandardCharsets.UTF_8);
            String cli = FreemarkerTemplateFactory.createCliTemplate(templateName, templateSource).render(parameters);
            return parse(CliSplitter.split(cli));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static GingesterConfiguration parse(String[] args) {

        GingesterConfiguration configuration = new GingesterConfiguration();

        List<String> syncFrom = List.of("__seed__");
        TransformerConfiguration previous = null;

        for (int i = 0; i < args.length; i++) {

            boolean markSyncFrom = false;
            boolean syncTo = false;
            boolean fsw = false;

            switch (args[i]) {

                case "+":
                    i++;
                    break;

                case "-b":
                case "--break":
                    return configuration;

                case "-r":
                case "--report":
                    configuration.report = Integer.parseInt(args[++i]);
                    break;

                case "-cf":
                case "--cli-file":
                    try {
                        String arg = args[++i];
                        String raw = Files.readString(Paths.get(arg));
                        args = splice(arg, raw, args, i);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);  // TODO
                    }
                    break;

                case "-cr":
                case "--cli-resource":
                    try {
                        String arg = args[++i];
                        String resource = Stream.of(arg, "/" + arg, "/gingester/" + arg)
                                .flatMap(s -> Stream.of(s, s + ".cli"))
                                .filter(s -> Main.class.getResourceAsStream(s) != null)
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + arg));
                        String raw = new String(Main.class.getResourceAsStream(resource).readAllBytes(), StandardCharsets.UTF_8);
                        args = splice(resource, raw, args, i);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);  // TODO
                    }
                    break;

                case "-l":
                case "--links":
                    requireNonNull(previous, "Found -l/--links before first transformer");
                    List<String> links = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].matches("[+-].*")) {
                        links.add(args[++i]);
                    }
                    previous.links(links);
                    break;

                case "-e":
                case "--excepts":
                    List<String> excepts = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].matches("[+-].*")) {
                        excepts.add(args[++i]);
                    }
                    if (previous != null) previous.excepts(excepts);
                    else configuration.excepts = excepts;
                    break;

                case "--":
                    requireNonNull(previous, "Found -- before first transformer");
                    previous.links(Collections.emptyList());
                    break;

                case "-sf":
                case "--sync-from":
                    syncFrom = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].matches("[+-].*")) {
                        syncFrom.add(args[++i]);
                    }
                    break;

                case "-sft":
                case "--sync-from-transformer":
                    markSyncFrom = true;
                case "-stt":
                case "--sync-to-transformer":
                    syncTo = !markSyncFrom;  // a bit of trickery to basically skip this case if we fell through the -sft case
                case "-stft":
                case "--sync-to-and-from-transformer":
                    if (!markSyncFrom && !syncTo) {
                        markSyncFrom = true;
                        syncTo = true;
                    }
                case "-f":
                case "--fetch":
                case "-fa":
                case "--fetch-all":
                case "-s":
                case "--stash":
                case "-w":
                case "--swap":
                    fsw = !markSyncFrom && !syncTo;  // same bit of trickery as above
                case "-t":
                case "--transformer":

                    TransformerConfiguration transformer = new TransformerConfiguration();
                    String name;

                    if (fsw) {
                        if (args[i].matches(".*f.*a")) name = "FetchAll";
                        else if (args[i].contains("f")) name = "Fetch";
                        else if (args[i].contains("w")) name = "Swap";
                        else name = "Stash";
                    } else {
                        String next = args[++i];
                        if (next.matches("[\\d.]+")) {
                            String[] parts = next.split("\\.");
                            if (parts.length > 0 && !parts[0].isEmpty()) transformer.maxWorkers(Integer.parseInt(parts[0]));
                            if (parts.length > 1 && !parts[1].isEmpty()) transformer.maxQueueSize(Integer.parseInt(parts[1]));
                            if (parts.length > 2 && !parts[2].isEmpty()) transformer.maxBatchSize(Integer.parseInt(parts[2]));
                            next = args[++i];
                        }
                        String[] parts = next.split(":");
                        if (parts[parts.length - 1].endsWith("!")) {
                            transformer.report(true);
                            parts[parts.length - 1] = parts[parts.length - 1].substring(0, parts[parts.length - 1].length() - 1);
                        }
                        if (parts.length == 1) {
                            name = parts[0];
                        } else {
                            transformer.id(parts[0]);
                            name = parts[1];
                        }
                    }

                    if (args.length > i + 1 && !args[i + 1].matches("[+-].*")) {
                        i++;
                        JsonNode parameters;
                        try {
                            parameters = OBJECT_READER.readTree(args[i]);
                        } catch (JsonProcessingException e) {
                            parameters = JsonNodeFactory.instance.textNode(args[i]);
                        }
                        transformer.transformer(name, TransformerFactory.instance(name, parameters));
                    } else {
                        transformer.transformer(name, TransformerFactory.instance(name));
                    }

                    if (syncTo) transformer.syncs(syncFrom);
                    if (markSyncFrom) syncFrom = List.of(transformer.getId().orElseGet(() -> transformer.getName().orElseThrow(() -> new IllegalStateException("Neither transformer name nor id were given"))));

                    previous = transformer;

                    configuration.transformers.add(transformer);

                    break;

                default: throw new IllegalArgumentException("Unexpected argument: " + args[i]);
            }
        }

        return configuration;
    }

    private static String[] splice(String templateName, String templateSource, String[] args, int i) throws JsonProcessingException {
        FreemarkerTemplateWrapper template = createCliTemplate(templateName, templateSource);
        if (args.length > i + 1 && !args[i + 1].matches("[+-].*")) {
            JsonNode parameters = OBJECT_READER.readTree(args[i + 1]);
            String cli = template.render(parameters);
            return splice(args, CliSplitter.split(cli), i + 1, 1);
        } else {
            String cli = template.render();
            return splice(args, CliSplitter.split(cli), i + 1, 0);
        }
    }

    private static String[] splice(String[] target, String[] items, int index, int lose) {
        String[] result = new String[target.length + items.length - lose];
        System.arraycopy(target, 0, result, 0, index);
        System.arraycopy(items, 0, result, index, items.length);
        System.arraycopy(target, index + lose, result, index + items.length, target.length - index - lose);
        return result;
    }
}
