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
    private final int limit;

    public Probe(Parameters parameters) {
        target = getTarget(parameters.target);
        limit = parameters.limit;
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

        if (limit > 0) {
            target.print(
                    "---- " + description + " ----\n" +
                    context.prettyStash(limit) + '\n' +
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
                rule(JsonNode::isInt, limit -> o("limit", limit));
                rule(JsonNode::isTextual, target -> o("target", target));
                rule(JsonNode::isArray, array -> o("target", array.get(0), "limit", array.get(1)));
            }
        }

        public String target = "out";
        public int limit = 10;
    }
}
