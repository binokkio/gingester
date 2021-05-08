package b.nana.technology.gingester.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class Main {

    public static void main(String[] args) throws JsonProcessingException {
        Gingester gingester = fromArgs(args);
        gingester.run();
    }

    static Gingester fromArgs(String[] args) throws JsonProcessingException {

        Gingester gingester = new Gingester();
        Transformer<?, ?> upstream = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {

                case "-t":
                case "--transformer":

                    String transformerName = args[++i];
                    JsonNode parameters = null;
                    if (args.length > i + 1 && !args[i + 1].startsWith("-")) parameters = Configuration.OBJECT_MAPPER.readTree(args[++i]);
                    Transformer<?, ?> transformer = Provider.instance(transformerName, parameters);
                    if (upstream != null) {
                        link(gingester, upstream, transformer);
                    }  // TODO there is an edge case here where only one transformer was specified
                    upstream = transformer;
            }
        }

        return gingester;
    }

    @SuppressWarnings("unchecked")  // checked at runtime
    private static <T> void link(Gingester gingester, Transformer<?, ?> from, Transformer<?, ?> to) {
        if (!to.inputClass.isAssignableFrom(from.outputClass)) {
            throw new IllegalArgumentException(from + " and " + to + " are incompatible");
        }
        gingester.link(
                (Transformer<?, T>) from,
                (Transformer<T, ?>) to
        );
    }
}
