package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.PrintStream;
import java.util.stream.Collectors;

@Names(1)
@Passthrough
public final class Probe implements Transformer<Object, Object> {

    private final FetchKey fetchDescription = new FetchKey("description");
    private final PrintStream target;
    private final boolean includeContext;

    public Probe(Parameters parameters) {
        target = getTarget(parameters.target);
        includeContext = parameters.includeContext;
    }

    private PrintStream getTarget(String target) {
        switch (target) {
            case "out": return System.out;
            case "err": return System.err;
            default: throw new IllegalArgumentException("Unexpected value for target: " + target);
        }
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        String description = context.fetchReverse(fetchDescription)
                .map(Object::toString)
                .collect(Collectors.joining(" :: "));

        if (includeContext) {
            target.print(
                    "---- " + description + " ----\n" +
                    context.prettyStash() + '\n' +
                    in + '\n' +
                    "-".repeat(description.length() + 10) + "\n\n"
            );
        } else {
            target.print(
                    "---- " + description + " ----\n" +
                    in + '\n' +
                    "-".repeat(description.length() + 10) + "\n\n"
            );
        }

        out.accept(context, in);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isBoolean, includeContext -> o("includeContext", includeContext));
                rule(JsonNode::isTextual, target -> o("target", target));
                rule(JsonNode::isArray, array -> o("target", array.get(0), "includeContext", array.get(1)));
            }
        }

        public String target = "out";
        public boolean includeContext = true;
    }
}
