package b.nana.technology.gingester.core.cli;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.FlowRunner;
import b.nana.technology.gingester.core.Main;
import b.nana.technology.gingester.core.Node;
import b.nana.technology.gingester.core.template.FreemarkerTemplateFactory;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

    public static void parse(FlowBuilder target, String template, Object parameters) {
        String cli = FreemarkerTemplateFactory.createCliTemplate("string", template).render(parameters);
        parse(target, CliSplitter.split(cli));
    }

    public static void parse(FlowBuilder target, URL template, Object parameters) {
        try (InputStream templateStream = template.openStream()) {
            String templateName = template.toString();
            String templateSource = new String(templateStream.readAllBytes(), StandardCharsets.UTF_8);
            String cli = FreemarkerTemplateFactory.createCliTemplate(templateName, templateSource).render(parameters);
            parse(target, CliSplitter.split(cli));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void parse(FlowBuilder target, String[] args) {

        for (int i = 0; i < args.length; i++) {

            boolean markSyncFrom = false;
            boolean syncTo = false;
            boolean fsw = false;

            switch (args[i]) {

                case "+":
                    i++;
                    break;

                case "++":
                    // ignore all args until the next occurrence of "++"
                    // noinspection StatementWithEmptyBody
                    while (++i < args.length && !args[i].equals("++"));
                    break;

                case "-r":
                case "--report":
                    target.setReportIntervalSeconds(Integer.parseInt(args[++i]));
                    break;

                case "-v":
                case "--view":
                    boolean viewBridges = i + 1 < args.length && !args[i + 1].matches("[+-].*") && Boolean.parseBoolean(args[++i]);
                    target.setGoal(viewBridges ? FlowRunner.Goal.VIEW_BRIDGES : FlowRunner.Goal.VIEW);
                    break;

                case "-d":
                case "--divert":
                    List<String> from = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].matches("[+-].*"))
                        from.add(args[++i]);
                    if (from.isEmpty()) throw new IllegalArgumentException("-d/--divert must be followed by at least 1 id");
                    target.divert(from);
                    break;

                case "-dm":
                case "--debug":
                case "--debug-mode":
                    target.enableDebugMode();
                    break;

                case "-gs":
                case "--graceful-sigint":
                    target.enableShutdownHook();
                    break;

                case "-cf":
                case "--cli-file":
                    i = handleCliFile(target, args, i);
                    break;

                case "-cr":
                case "--cli-resource":
                    i = handleCliResource(target, args, i);
                    break;

                case "-l":
                case "--links":
                    List<String> links = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].matches("[+-].*"))
                        links.add(args[++i]);
                    if (links.isEmpty()) throw new IllegalArgumentException("-l/--links must be followed by at least 1 id");
                    target.linkTo(links);
                    break;

                case "-lf":
                case "--link-from":
                    List<String> linkFrom = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].matches("[+-].*"))
                        linkFrom.add(args[++i]);
                    target.linkFrom(linkFrom);
                    break;

                case "-e":
                case "--excepts":
                    List<String> excepts = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].matches("[+-].*"))
                        excepts.add(args[++i]);
                    if (excepts.isEmpty()) excepts.add("__elog__");
                    target.exceptTo(excepts);
                    break;

                case "-p":
                case "--probe":
                    args = splice(args, new String[] { "-t", "Probe" }, i + 1, 0);
                    break;

                case "-sf":
                case "--sync-from":
                    List<String> syncFrom = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].matches("[+-].*"))
                        syncFrom.add(args[++i]);
                    target.syncFrom(syncFrom);
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
                case "-fg":
                case "--finish-gate":
                case "-sfg":
                case "--seed-finish-gate":
                case "-s":
                case "--stash":
                case "-w":
                case "--swap":
                    fsw = !markSyncFrom && !syncTo;  // same bit of trickery as above
                case "-t":
                case "--transformer":

                    Node transformer = new Node();
                    String name;

                    if (fsw) {
                        if (args[i].matches(".*f.*a")) name = "FetchAll";
                        else if (args[i].matches(".*f.*g")) {
                            name = "FinishGate";
                            syncTo = !args[i].contains("s");  // TODO this is broken right? --finish-gate contains "s"
                        }
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

                    ArrayNode parameters = JsonNodeFactory.instance.arrayNode();
                    while (args.length > i + 1 && !args[i + 1].matches("[+-].*")) {
                        i++;
                        try {
                            parameters.add(OBJECT_READER.readTree(args[i]));
                        } catch (JsonProcessingException e) {
                            parameters.add(JsonNodeFactory.instance.textNode(args[i]));
                        }
                    }

                    if (parameters.size() > 1) {
                        transformer.name(name).transformer(TransformerFactory.instance(name, parameters));
                    } else if (!parameters.isEmpty()) {
                        transformer.name(name).transformer(TransformerFactory.instance(name, parameters.get(0)));
                    } else {
                        transformer.name(name).transformer(TransformerFactory.instance(name));
                    }

                    target.add(transformer);
                    if (syncTo) target.sync();
                    if (markSyncFrom) target.syncFrom(target.getLastId());

                    break;

                case "-ss":
                case "--stash-string":
                    args = splice(args, new String[] { "-t", "StashString" }, i + 1, 0);
                    break;

                case "--":
                    target.linkFrom(List.of());
                    break;

                default: throw new IllegalArgumentException("Unexpected argument: " + args[i]);
            }
        }
    }

    private static int handleCliFile(FlowBuilder target, String[] args, int i) {

        ScopedSource scopedSource = new ScopedSource(args[++i]);
        URL url;
        try {
            url = Paths.get(scopedSource.source).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);  // TODO
        }

        return handleCliUrl(target, scopedSource, url, args, i);
    }

    private static int handleCliResource(FlowBuilder target, String[] args, int i) {

        ScopedSource scopedSource = new ScopedSource(args[++i]);
        URL url = Stream.of(scopedSource.source, "/" + scopedSource.source, "/gingester/" + scopedSource.source)
                .flatMap(s -> Stream.of(s, s + ".cli"))
                .map(Main.class::getResource)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + scopedSource.source));

        return handleCliUrl(target, scopedSource, url, args, i);
    }

    private static class ScopedSource {

        private final String scope;
        private final String source;

        private ScopedSource(String arg) {
            if (arg.contains(":")) {
                String[] parts = arg.split(":", 2);
                scope = parts[0];
                source = parts[1];
            } else {
                scope = null;
                source = arg;
            }
        }
    }

    private static int handleCliUrl(FlowBuilder target, ScopedSource scopedSource, URL url, String[] args, int i) {

        if (scopedSource.scope != null)
            target.enterScope(scopedSource.scope);

        int returnValue;

        if (args.length > i + 1 && !args[i + 1].matches("[+-].*")) {

            JsonNode parameters;
            try {
                parameters = OBJECT_READER.readTree(args[i + 1]);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            target.cli(url, parameters);
            returnValue =  i + 1;

        } else {
            target.cli(url);
            returnValue =  i;
        }

        if (scopedSource.scope != null)
            target.exitScope();

        return returnValue;
    }

    private static String[] splice(String[] target, String[] items, int index, int lose) {
        String[] result = new String[target.length + items.length - lose];
        System.arraycopy(target, 0, result, 0, index);
        System.arraycopy(items, 0, result, index, items.length);
        System.arraycopy(target, index + lose, result, index + items.length, target.length - index - lose);
        return result;
    }
}
