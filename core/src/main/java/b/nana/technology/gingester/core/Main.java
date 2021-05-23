package b.nana.technology.gingester.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Main {

    public static void main(String[] args) {

        final Gingester gingester = fromArgs(args);
        final Thread main = Thread.currentThread();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            gingester.signalShutdown();
            try {
                main.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        Set<String> argSet = new HashSet<>(Arrays.asList(args));

        if (argSet.contains("-pc") || argSet.contains("--print-config")) {
            System.out.println(gingester.toConfiguration().toJson());
        } else {
            gingester.run();
        }
    }

    static Gingester fromArgs(String[] args) {

        Gingester gingester = new Gingester();
        Transformer<?, ?> upstream = null;
        Transformer<?, ?> syncFrom = null;

        for (int i = 0; i < args.length; i++) {

            boolean markSyncFrom = false;
            boolean syncTo = false;
            boolean syncLink = false;

            switch (args[i]) {

                case "-pc":
                case "--print-config":
                    // ignore
                    break;

                case "-sft":
                case "--sync-from-transformer":
                    markSyncFrom = true;
                case "-stt":
                case "--sync-to-transformer":
                    syncTo = !markSyncFrom;  // bit of trickery to basically skip this case if we fell through the -sft case
                case "-slt":
                case "--synced-link-transformer":
                    syncLink = !markSyncFrom && !syncTo && syncFrom == null;  // similar trickery as above
                case "-t":
                case "--transformer":
                    String transformerName = args[++i];
                    JsonNode parameters = null;
                    if (args.length > i + 1 && !args[i + 1].startsWith("-")) {
                        try {
                            parameters = Configuration.OBJECT_MAPPER.readTree(args[++i]);
                        } catch (JsonProcessingException e) {
                            parameters = JsonNodeFactory.instance.textNode(args[i]);
                        }
                    }
                    Transformer<?, ?> transformer = Provider.instance(transformerName, parameters);
                    if (upstream != null) {
                        link(gingester, upstream, transformer, syncLink);
                    } else {
                        gingester.name(transformerName, transformer);  // TODO this is a temp fix for when only 1 transformer is specified
                    }
                    upstream = transformer;
                    if (markSyncFrom) {
                        syncFrom = transformer;
                    } else if (syncTo) {
                        gingester.sync(syncFrom, transformer);
                        syncFrom = null;
                    }
                    break;

                default: throw new IllegalArgumentException("Unexpected argument: " + args[i]);
            }
        }

        if (syncFrom != null) {
            throw new IllegalArgumentException("Unmatched -sft/--sync-from-transformer, missing -stt/--sync-to-transformer");
        }

        return gingester;
    }

    @SuppressWarnings("unchecked")  // checked at runtime in gingester.link()
    private static <T> void link(Gingester gingester, Transformer<?, ?> from, Transformer<?, ?> to, boolean syncLink) {
        Link<?> link = gingester.link(
                (Transformer<?, T>) from,
                (Transformer<T, ?>) to
        );
        if (syncLink) link.sync();
    }
}
