package b.nana.technology.gingester.core.cli;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.configuration.GingesterConfiguration;
import b.nana.technology.gingester.core.configuration.TransformerConfiguration;
import b.nana.technology.gingester.core.freemarker.FreemarkerJacksonWrapper;
import b.nana.technology.gingester.core.freemarker.FreemarkerTemplateFactory;
import b.nana.technology.gingester.core.freemarker.FreemarkerTemplateWrapper;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        if (args.length > 0) {
            GingesterConfiguration configuration = parseArgs(args);
            if (configuration.report == null) configuration.report = 2;
            Gingester gingester = new Gingester();
            configuration.applyTo(gingester);
            gingester.run();
        } else {
            printHelp();
        }
    }

    public static GingesterConfiguration parseArgs(String[] args) {

        boolean break_ = false;

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
                    break_ = true;
                    break;

                case "-r":
                case "--report":
                    configuration.report = Integer.parseInt(args[++i]);
                    break;

                case "-cf":
                case "--cli-file":
                    try {
                        args = splice(Files.readString(Paths.get(args[++i])), args, i);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);  // TODO
                    }
                    break;

                case "-cr":
                case "--cli-resource":
                    try {
                        String resource = args[++i];
                        InputStream resourceStream = Stream.of(resource, "/" + resource, "/gingester/" + resource)
                                .flatMap(s -> Stream.of(s, s + ".cli"))
                                .map(Main.class::getResourceAsStream)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resource));
                        args = splice(new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8), args, i);
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
                    syncTo = !markSyncFrom;  // bit of trickery to basically skip this case if we fell through the -sft case
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
                    String transformerName;

                    if (fsw) {
                        if (args[i].matches(".*f.*a")) transformerName = "FetchAll";
                        else if (args[i].contains("f")) transformerName = "Fetch";
                        else if (args[i].contains("w")) transformerName = "Swap";
                        else transformerName = "Stash";
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
                            transformerName = parts[0];
                        } else {
                            transformer.id(parts[0]);
                            transformerName = parts[1];
                        }
                    }

                    if (args.length > i + 1 && !args[i + 1].matches("[+-].*")) {
                        transformer.transformer(transformerName, args[++i]);
                    } else {
                        transformer.transformer(transformerName);
                    }

                    if (markSyncFrom) {
                        syncFrom = List.of(transformer.getId().orElseGet(() -> transformer.getName().orElseThrow(() -> new IllegalStateException("Neither transformer name nor id were given"))));
                    } else if (syncTo) {
                        transformer.syncs(syncFrom);
                    }

                    previous = transformer;

                    if (break_) break;

                    configuration.transformers.add(transformer);

                    break;

                case "-h":
                case "--help":
                    printHelp();
                    System.exit(0);

                default: throw new IllegalArgumentException("Unexpected argument: " + args[i]);
            }
        }

        return configuration;
    }

    private static void printHelp() {

        try {
            System.out.println(new String(requireNonNull(
                    Main.class.getResourceAsStream("/gingester/core/help.txt"),
                    "/gingester/core/help.txt resource missing"
            ).readAllBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        System.out.println("\nAvailable transformers:\n");
        TransformerFactory.getTransformerHelps().forEach(help ->
                System.out.println("    " + help));
    }

    private static String[] splice(String raw, String[] args, int i) throws JsonProcessingException {
        FreemarkerTemplateWrapper template = FreemarkerTemplateFactory.createAlternateSyntaxTemplate(raw, FreemarkerJacksonWrapper::new);
        if (args.length > i + 1 && !args[i + 1].matches("[+-].*")) {
            JsonNode parameters = GingesterConfiguration.OBJECT_READER.readTree(args[i + 1]);
            String cli = template.render(parameters);
            return splice(args, CliParser.parse(cli), i + 1, 1);
        } else {
            String cli = template.render();
            return splice(args, CliParser.parse(cli), i + 1, 0);
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
