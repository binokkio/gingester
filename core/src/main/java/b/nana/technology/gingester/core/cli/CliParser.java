package b.nana.technology.gingester.core.cli;

import b.nana.technology.gingester.core.*;
import b.nana.technology.gingester.core.template.FreemarkerTemplateFactory;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static b.nana.technology.gingester.core.cli.BlockCommentRemover.removeBlockComments;

public final class CliParser {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final Logger LOGGER = LoggerFactory.getLogger(CliParser.class);

    private CliParser() {}

    public static void parse(FlowBuilder target, String template, Object kwargs) {
        String cli = FreemarkerTemplateFactory.createCliTemplate("string", removeBlockComments(template)).render(kwargs);
        parseCleanArgs(target, CliSplitter.split(cli));
    }

    public static void parse(FlowBuilder target, URL template, Object kwargs) {
        try (InputStream templateStream = template.openStream()) {
            String templateName = template.toString();
            String templateSource = removeBlockComments(new String(templateStream.readAllBytes(), StandardCharsets.UTF_8));
            String cli = FreemarkerTemplateFactory.createCliTemplate(templateName, templateSource).render(kwargs);
            parseCleanArgs(target, CliSplitter.split(cli));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void parse(FlowBuilder target, String[] args) {
        parseCleanArgs(target, removeBlockComments(args));
    }

    private static void parseCleanArgs(FlowBuilder target, String[] args) {

        for (int i = 0; i < args.length; i++) {

            switch (args[i]) {

                case "-dm":
                case "--debug":
                case "--debug-mode":
                    target.enableDebugMode();
                    break;

                case "-gs":
                case "--graceful-sigint":
                    target.enableShutdownHook();
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

                case "-cf":
                case "--cli-file":
                    i = handleCliFile(target, args, i);
                    break;

                case "-cr":
                case "--cli-resource":
                    i = handleCliResource(target, args, i);
                    break;

                case "-d":
                case "--divert":
                    List<String> from = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].matches("[+-].*"))
                        from.add(args[++i]);
                    if (from.isEmpty()) throw new IllegalArgumentException("-d/--divert must be followed by at least 1 id");
                    target.divert(from);
                    break;

                case "-e":
                case "--excepts":
                    List<String> excepts = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].matches("[+-].*")) {
                        String arg = args[++i];
                        if (Character.isLowerCase(arg.charAt(0)))
                            arg = target.getElog(arg);
                        excepts.add(arg);
                    }
                    if (excepts.isEmpty()) excepts.add(Id.ELOG.getGlobalId());
                    target.exceptTo(excepts);
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

                case "-sf":
                case "--sync-from":
                    List<String> syncFrom = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].matches("[+-].*"))
                        syncFrom.add(args[++i]);
                    target.syncFrom(syncFrom);
                    break;

                case "-sfs":
                case "--sync-from-stash":
                    i = addNode(target.node().name("Stash"), args, i, false);
                    target.syncFrom(target.getLastId().getGlobalId());
                    break;

                case "-sft":
                case "--sync-from-transformer":
                    i = addNode(target.node(), args, i, true);
                    target.syncFrom(target.getLastId().getGlobalId());
                    break;

                case "-stf":
                case "--sync-to-fetch":
                    i = addNode(target.node().name("OnFinishFetch"), args, i, false);
                    target.sync();
                    break;

                case "-stof":
                case "--sync-to-on-finish":
                    i = addNode(target.node().name("OnFinish"), args, i, false);
                    target.sync();
                    break;

                case "-stt":
                case "--sync-to-transformer":
                    i = addNode(target.node(), args, i, true);
                    target.sync();
                    break;

                case "-stft":
                case "--sync-to-and-from-transformer":
                    i = addNode(target.node(), args, i, true);
                    target.sync();
                    target.syncFrom(target.getLastId().getGlobalId());
                    break;

                case "-sfpt":
                case "--sync-from-point":
                case "--sync-from-passthrough":
                    i = addNode(target.node().name("Passthrough"), args, i, true);
                    target.syncFrom(target.getLastId().getGlobalId());
                    break;

                case "-pt":
                case "--point":
                case "--passthrough":
                    i = addNode(target.node().name("Passthrough"), args, i, true);
                    break;

                case "-t":
                case "--transformer":
                    i = addNode(target.node(), args, i, true);
                    break;

                case "-a":
                case "--as":
                    i = addNode(target.node().name("As"), args, i, false);
                    break;

                case "-f":
                case "--fetch":
                    i = addNode(target.node().name("Fetch"), args, i, false);
                    break;

                case "-i":
                case "--is":
                    i = addNode(target.node().name("Is"), args, i, false);
                    break;

                case "-p":
                case "--probe":
                    i = addNode(target.node().name("Probe"), args, i, false);
                    break;

                case "-s":
                case "--stash":
                    i = addNode(target.node().name("Stash"), args, i, false);
                    break;

                case "-ss":
                case "--stash-string":
                    i = addNode(target.node().name("StashString"), args, i, false);
                    break;

                case "-w":
                case "--swap":
                    i = addNode(target.node().name("Swap"), args, i, false);
                    break;

                case "-wi":
                case "--wormhole-in":
                    i = addNode(target.node().name("WormholeIn"), args, i, false);
                    break;

                case "-wo":
                case "--wormhole-out":
                    i = addNode(target.node().name("WormholeOut"), args, i, false);
                    break;

                case "--":
                    target.linkFrom(List.of());
                    break;

                case "+":  // TODO remove, replace `.matches("[+-].*")` with ~ `.charAt(0) != '-'`
                    LOGGER.warn("The CLI arg \"+\" will be removed in a future version, use block style comments instead");
                    i++;
                    break;

                case "-fa":
                case "--fetch-all":
                    LOGGER.warn("The CLI arg \"" + args[i] + "\" will be removed in a future version, use `-t FetchAll` instead");
                    i = addNode(target.node().name("FetchAll"), args, i, false);
                    break;

                case "-fg":
                case "--finish-gate":  // TODO remove
                    LOGGER.warn("The CLI arg \"" + args[i] + "\" will be removed in a future version, use `-stt FinishGate` instead");
                    i = addNode(target.node().name("FinishGate"), args, i, false);
                    target.sync();
                    break;

                case "-sfg":
                case "--seed-finish-gate":  // TODO remove
                    LOGGER.warn("The CLI arg \"" + args[i] + "\" will be removed in a future version, use `-t FinishGate` instead");
                    i = addNode(target.node().name("FinishGate"), args, i, false);
                    break;

                default: throw new IllegalArgumentException("Unexpected argument: " + args[i]);
            }
        }
    }

    private static int addNode(Node node, String[] args, int i, boolean maybeNamed) {

        if (maybeNamed && args.length > i + 1 && args[i + 1].matches("[\\d.]+")) {
            String[] parts = args[++i].split("\\.");
            if (parts.length > 0 && !parts[0].isEmpty()) node.maxWorkers(Integer.parseInt(parts[0]));
            if (parts.length > 1 && !parts[1].isEmpty()) node.maxQueueSize(Integer.parseInt(parts[1]));
            if (parts.length > 2 && !parts[2].isEmpty()) node.maxBatchSize(Integer.parseInt(parts[2]));
        }

        if (maybeNamed && args.length > i + 1 && Character.isUpperCase(args[i + 1].charAt(0))) {

            String arg = args[++i];
            if (arg.endsWith("!")) {
                node.report(true);
                arg = arg.substring(0, arg.length() - 1);
            }

            String[] parts = arg.split(":");
            if (parts.length == 2) {
                node.id(parts[0]);
                node.name(parts[1]);
            } else if (node.getName().isPresent()) {
                node.id(parts[0]);
            } else {
                node.name(parts[0]);
            }
        }

        ArrayNode parameters = JsonNodeFactory.instance.arrayNode();
        while (args.length > i + 1 && !args[i + 1].matches("[+-].*")) {
            i++;
            if (args[i].equals("%") && !args[i + 1].matches("[+-].*") && !parameters.isEmpty()) {
                try {
                    OBJECT_MAPPER
                            .readerForUpdating(parameters.get(parameters.size() - 1))
                            .readValue(args[++i]);
                } catch (JsonProcessingException e) {
                    throw new IllegalArgumentException(e);
                }
            } else if (args[i].equals("@")) {
                parameters.add(JsonNodeFactory.instance.textNode(args[++i]));
            } else {
                try {
                    parameters.add(OBJECT_MAPPER.readTree(args[i]));
                } catch (JsonProcessingException e) {
                    parameters.add(JsonNodeFactory.instance.textNode(args[i]));
                }
            }
        }

        if (parameters.size() > 1) {
            node.transformer(TransformerFactory.instance(node.getName().orElseThrow(), parameters));
        } else if (!parameters.isEmpty()) {
            node.transformer(TransformerFactory.instance(node.getName().orElseThrow(), parameters.get(0)));
        } else {
            node.transformer(TransformerFactory.instance(node.getName().orElseThrow()));
        }

        node.add();
        return i;
    }

    private static int handleCliFile(FlowBuilder target, String[] args, int i) {

        ScopedSource scopedSource = new ScopedSource(target, args[++i]);
        URL url;
        try {
            url = Paths.get(scopedSource.source).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);  // TODO
        }

        return handleCliUrl(target, scopedSource, url, args, i);
    }

    private static int handleCliResource(FlowBuilder target, String[] args, int i) {

        ScopedSource scopedSource = new ScopedSource(target, args[++i]);
        URL url = Stream.of(scopedSource.source, "/" + scopedSource.source, "/gingester/" + scopedSource.source)
                .flatMap(s -> Stream.of(s, s + ".gcli", s + ".cli"))
                .map(Main.class::getResource)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + scopedSource.source));

        return handleCliUrl(target, scopedSource, url, args, i);
    }

    private static int handleCliUrl(FlowBuilder target, ScopedSource scopedSource, URL url, String[] args, int i) {

        if (scopedSource.scope != null)
            target.enterScope(scopedSource.scope);

        if (args.length > i + 1 && !args[i + 1].matches("[+-].*")) {

            JsonNode kwargs;
            try {
                kwargs = OBJECT_MAPPER.readTree(args[++i]);
                while (args.length > i + 1 && args[i + 1].equals("%")) {
                    OBJECT_MAPPER
                            .readerForUpdating(kwargs)
                            .readValue(args[i += 2]);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            target.cli(url, kwargs);

        } else {
            target.cli(url);
        }

        if (scopedSource.scope != null)
            target.exitScope();

        return i;
    }

    private static class ScopedSource {

        private final String scope;
        private final String source;

        private ScopedSource(FlowBuilder target, String arg) {
            if (arg.startsWith(":")) {
                scope = target.getLastId().getLocalId();
                source = arg.substring(1);
            } else if (arg.contains(":")) {
                String[] parts = arg.split(":", 2);
                scope = parts[0];
                source = parts[1];
            } else {
                scope = null;
                source = arg;
            }
        }
    }
}
