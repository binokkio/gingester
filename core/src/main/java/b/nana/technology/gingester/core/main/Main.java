package b.nana.technology.gingester.core.main;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.configuration.Configuration;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        Configuration configuration = parseArgs(args);
        Gingester gingester = new Gingester();
        configuration.applyTo(gingester);
        gingester.run();
    }

    static Configuration parseArgs(String[] args) {

        boolean break_ = false;
        boolean printConfig = false;

        Configuration configuration = new Configuration();
        configuration.report = true;

        String syncFrom = "__seed__";
        b.nana.technology.gingester.core.controller.Configuration previous = null;

        for (int i = 0; i < args.length; i++) {

            boolean markSyncFrom = false;
            boolean syncTo = false;
            boolean fsw = false;

            switch (args[i]) {

                case "-b":
                case "--break":
                    break_ = true;
                    break;

                case "-pc":
                case "--print-config":
                    printConfig = true;
                    break;

                case "-nr":
                case "--no-report":
                    configuration.report = false;
                    break;

                case "-fc":
                case "--from-config":
                case "--file-config":
                    try {
                        Configuration append = Configuration.fromJson(Files.newInputStream(Paths.get(args[++i])));
                        configuration.append(append);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);  // TODO
                    }
                    break;

                case "-rc":
                case "--resource-config":
                    try {
                        String resource = args[++i];
                        InputStream resourceStream = Stream.of(resource, "/gingester/rc/" + resource, "/gingester/rc/" + resource + ".json")
                                .map(Main.class::getResourceAsStream)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resource));
                        Configuration append = Configuration.fromJson(resourceStream);
                        configuration.append(append);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);  // TODO
                    }
                    break;

                case "-l":
                case "--link":
                    requireNonNull(previous, "Found -l/--links before first transformer");
                    List<String> links = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        links.add(args[++i]);
                    }
                    previous.links(links);
                    break;

                case "--":
                    requireNonNull(previous, "Found -- before first transformer");
                    previous.links(Collections.emptyList());
                    break;

                case "-sft":
                case "--sync-from-transformer":
                    markSyncFrom = true;
                case "-stt":
                case "--sync-to-transformer":
                    syncTo = !markSyncFrom;  // bit of trickery to basically skip this case if we fell through the -sft case

                case "-f":
                case "--fetch":
                case "-s":
                case "--stash":
                case "-w":
                case "--swap":
                    fsw = !markSyncFrom && !syncTo;  // same bit of trickery as above
                case "-t":
                case "--transformer":

                    b.nana.technology.gingester.core.controller.Configuration controller = new b.nana.technology.gingester.core.controller.Configuration();

                    if (fsw) {
                        if (args[i].contains("f")) controller.transformer("Fetch");
                        else if (args[i].contains("w")) controller.transformer("Swap");
                        else controller.transformer("Stash");
                    } else {
                        String next = args[++i];
                        if (next.matches("\\d+")) {
                            controller.async(true);
                            controller.maxWorkers(Integer.parseInt(next));
                            next = args[++i];
                        }
                        String[] parts = next.split(":");
                        if (parts.length == 1) {
                            controller.transformer(parts[0]);
                        } else {
                            controller.id(parts[0]);
                            controller.transformer(parts[1]);
                        }
                    }

                    if (args.length > i + 1 && !args[i + 1].startsWith("-")) {
                        try {
                            controller.parameters(Configuration.OBJECT_READER.readTree(args[++i]));
                        } catch (JsonProcessingException e) {
                            controller.parameters(JsonNodeFactory.instance.textNode(args[i]));
                        }
                    }

                    if (markSyncFrom) {
                        syncFrom = controller.getId() != null ? controller.getId() : controller.getTransformer();
                    } else if (syncTo) {
                        controller.syncs(List.of(syncFrom));
                    }

                    previous = controller;

                    if (break_) break;

                    configuration.transformers.add(controller);

                    break;

                case "-h":
                case "--help":
                    try {
                        System.out.println(new String(requireNonNull(
                                Main.class.getResourceAsStream("/gingester/core/help.txt"),
                                "/gingester/core/help.txt resource missing"
                        ).readAllBytes()));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    System.out.println("\nAvailable transformers:\n");
                    TransformerFactory.getTransformers().forEach(name ->
                            System.out.println("    " + name));
                    System.exit(0);

                default: throw new IllegalArgumentException("Unexpected argument: " + args[i]);
            }
        }

        if (printConfig) {
            System.out.println(configuration.toJson());
            System.exit(0);
        }

        return configuration;
    }
}
