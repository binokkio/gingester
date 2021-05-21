package b.nana.technology.gingester.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

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

        gingester.run();
    }

    static Gingester fromArgs(String[] args) {

        Gingester gingester = new Gingester();
        Transformer<?, ?> upstream = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {

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
                        link(gingester, upstream, transformer);
                    }  // TODO there is an edge case here where only one transformer was specified
                    upstream = transformer;
            }
        }

        return gingester;
    }

    @SuppressWarnings("unchecked")  // checked at runtime in gingester.link()
    private static <T> void link(Gingester gingester, Transformer<?, ?> from, Transformer<?, ?> to) {
        gingester.link(
                (Transformer<?, T>) from,
                (Transformer<T, ?>) to
        );
    }
}
