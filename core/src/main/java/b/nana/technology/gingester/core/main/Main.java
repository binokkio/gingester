package b.nana.technology.gingester.core.main;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.configuration.Configuration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class Main {

    private Main() {}

    public static void main(String[] args) {

        boolean printConfig = false;

        Configuration configuration = new Configuration();
        configuration.report = true;

        String syncFrom = "__seed__";

        for (int i = 0; i < args.length; i++) {

            boolean markSyncFrom = false;
            boolean syncTo = false;

            switch (args[i]) {

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
                    Path path = Paths.get(args[++i]);
                    try {
                        Configuration append = Configuration.fromJson(Files.newInputStream(path));
                        configuration.report = append.report;
                        configuration.transformers.addAll(append.transformers);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);  // TODO
                    }
                    break;

                case "-sft":
                case "--sync-from-transformer":
                    markSyncFrom = true;
                case "-stt":
                case "--sync-to-transformer":
                    syncTo = !markSyncFrom;  // bit of trickery to basically skip this case if we fell through the -sft case
                case "-t":
                case "--transformer":

                    // TODO -s for -t Stash and -f for -t Fetch

                    b.nana.technology.gingester.core.controller.Configuration controller = new b.nana.technology.gingester.core.controller.Configuration();

                    String next = args[++i];

                    if (next.matches("\\d+")) {
                        controller.async(true);
                        controller.maxWorkers(Integer.parseInt(next));
                        controller.transformer(args[++i]);
                    } else {
                        controller.transformer(next);
                    }

                    if (args.length > i + 1 && !args[i + 1].startsWith("-")) {
                        try {
                            controller.parameters(Configuration.OBJECT_READER.readTree(args[++i]));
                        } catch (JsonProcessingException e) {
                            controller.parameters(JsonNodeFactory.instance.textNode(args[i]));
                        }
                    }

                    if (markSyncFrom) {
                        syncFrom = controller.getTransformer();
                    } else if (syncTo) {
                        controller.syncs(List.of(syncFrom));
                    }

                    configuration.transformers.add(controller);

                    break;

                default: throw new IllegalArgumentException("Unexpected argument: " + args[i]);
            }
        }

        if (printConfig) {
            System.out.println(configuration.toJson());
        } else {
            Gingester gingester = new Gingester();
            configuration.applyTo(gingester);
            gingester.run();
        }
    }
}
