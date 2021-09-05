package b.nana.technology.gingester.core.main;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.controller.Configuration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class Main {

    private Main() {}

    public static void main(String[] args) {

        boolean printConfig = false;

        b.nana.technology.gingester.core.configuration.Configuration configuration = new b.nana.technology.gingester.core.configuration.Configuration();
        configuration.report = true;

        String syncFrom = "__seed__";

        for (int i = 0; i < args.length; i++) {

            boolean markSyncFrom = false;
            boolean syncTo = false;
            boolean async = false;

            switch (args[i]) {

                case "-pc":
                case "--print-config":
                    printConfig = true;
                    break;

                case "-fc":
                case "--from-config":
                case "--file-config":
                    Path path = Paths.get(args[++i]);
                    try {
                        b.nana.technology.gingester.core.configuration.Configuration append = b.nana.technology.gingester.core.configuration.Configuration.fromJson(Files.newInputStream(path));
                        configuration.report = append.report;
                        configuration.transformers.addAll(append.transformers);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);  // TODO
                    }
                    break;

                case "-nr":
                case "--no-report":
                    configuration.report = false;
                    break;

                case "-sft":
                case "--sync-from-transformer":
                    markSyncFrom = true;
                case "-stt":
                case "--sync-to-transformer":
                    syncTo = !markSyncFrom;  // bit of trickery to basically skip this case if we fell through the -sft case
                case "-at":
                case "--async-transformer":
                    async = !markSyncFrom && !syncTo;  // similar trickery as above
                case "-t":
                case "--transformer":

                    Configuration parameters = new Configuration();

                    // TODO -s for -t Stash and -f for -t Fetch

                    parameters.transformer(args[++i]);
                    if (async) parameters.async(true);
                    if (args.length > i + 1 && !args[i + 1].startsWith("-")) {
                        try {
                            parameters.parameters(b.nana.technology.gingester.core.configuration.Configuration.OBJECT_READER.readTree(args[++i]));
                        } catch (JsonProcessingException e) {
                            parameters.parameters(JsonNodeFactory.instance.textNode(args[i]));
                        }
                    }

                    if (markSyncFrom) {
                        syncFrom = parameters.getTransformer();
                    } else if (syncTo) {
                        List<String> syncs = new ArrayList<>(parameters.getSyncs());
                        syncs.add(syncFrom);
                        parameters.syncs(syncs);
                    }

                    configuration.transformers.add(parameters);

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
