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

        Configuration syncFrom = null;

        for (int i = 0; i < args.length; i++) {

            boolean markSyncFrom = false;
            boolean syncTo = false;
            boolean asyncLink = false;

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
                    asyncLink = !markSyncFrom && !syncTo;  // similar trickery as above
                case "-t":
                case "--transformer":


                    Configuration parameters = new Configuration();

                    // TODO -s for -t Stash and -f for -t Fetch

                    parameters.transformer(args[++i]);
                    if (args.length > i + 1 && !args[i + 1].startsWith("-")) {
                        try {
                            parameters.parameters(b.nana.technology.gingester.core.configuration.Configuration.OBJECT_READER.readTree(args[++i]));
                        } catch (JsonProcessingException e) {
                            parameters.parameters(JsonNodeFactory.instance.textNode(args[i]));
                        }
                    }

                    configuration.transformers.add(parameters);

                    if (markSyncFrom) {
                        syncFrom = parameters;
                    } else if (syncTo) {
                        if (syncFrom == null) throw new IllegalArgumentException("Unmatched -stt/--sync-to-transformer");
                        List<String> syncs = new ArrayList<>(syncFrom.getSyncs());
                        syncs.add(parameters.getTransformer());
                        syncFrom.syncs(syncs);
                        syncFrom = null;
                    }
                    break;

                default: throw new IllegalArgumentException("Unexpected argument: " + args[i]);
            }
        }

        if (syncFrom != null) {
            throw new IllegalArgumentException("Unmatched -sft/--sync-from-transformer");
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
