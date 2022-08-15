package b.nana.technology.gingester.core.transformers.stash;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.OutputFetcher;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Names(1)
public final class Fetch implements Transformer<Object, Object>, OutputFetcher {

    private final FetchKey fetchKey;
    private final boolean optional;

    public Fetch(Parameters parameters) {
        fetchKey = new FetchKey(parameters.fetchKey);
        optional =parameters.optional;
    }

    @Override
    public FetchKey getOutputStashName() {
        return fetchKey;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        if (!optional) {
            out.accept(
                    context,
                    context.require(fetchKey)
            );
        } else {
            context.fetch(fetchKey).ifPresent(value ->
                    out.accept(
                            context,
                            value
                    ));
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, fetchKey -> o("fetchKey", fetchKey));
                rule(JsonNode::isArray, array -> {
                    if (array.size() == 2 && array.get(1).asText().equals("optional")) {
                        return o("fetchKey", array.get(0), "optional", true);
                    } else {
                        throw new IllegalArgumentException("Invalid parameters: " + array);
                    }
                });
            }
        }

        public String fetchKey = "stash";
        public boolean optional;

        @JsonCreator
        public Parameters() {

        }

        public Parameters(String fetchKey) {
            this.fetchKey = fetchKey;
        }
    }
}
