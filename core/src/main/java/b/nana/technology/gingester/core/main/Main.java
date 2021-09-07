package b.nana.technology.gingester.core.main;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.configuration.Configuration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class Main {

    private Main() {}

    public static void main(String[] args) {

        boolean break_ = false;
        boolean printConfig = false;

        Configuration configuration = new Configuration();
        configuration.report = true;

        String syncFrom = "__seed__";

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
                        Configuration append = Configuration.fromJson(Main.class.getResourceAsStream(args[++i]));
                        configuration.append(append);
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
                            controller.transformer(args[++i]);
                        } else {
                            controller.transformer(next);
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
                        syncFrom = controller.getTransformer();
                    } else if (syncTo) {
                        controller.syncs(List.of(syncFrom));
                    }

                    if (break_) break;

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
