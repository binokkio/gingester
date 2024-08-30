package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Names(1)
@Passthrough
@Example(example = "3 interrupt", description = "Pass the first 3 items and interrupt for the rest")
public final class Head implements Transformer<Object, Object> {

    private final ContextMap<int[]> state = new ContextMap<>();

    private final int limit;
    private final boolean interrupt;

    public Head(Parameters parameters) {
        limit = parameters.limit;
        interrupt = parameters.interrupt;
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        state.put(context, new int[1]);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        boolean yield = state.apply(context, holder -> {
            if (holder[0] < limit) {
                holder[0]++;
                return true;
            } else {
                return false;
            }
        });

        if (yield) {
            out.accept(context, in);
        } else if (interrupt) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        state.remove(context);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, limit -> o("limit", limit));
                rule(JsonNode::isTextual, text -> {
                    if (text.textValue().equals("interrupt")) return o("interrupt", true);
                    else throw new IllegalArgumentException("Unexpected argument: " + text);
                });
                rule(JsonNode::isArray, array -> {
                    if (array.get(0).isInt() && array.get(1).textValue().equals("interrupt")) return o("limit", array.get(0), "interrupt", true);
                    else throw new IllegalArgumentException("Unexpected argument: " + array);
                });
            }
        }

        public int limit;
        public boolean interrupt;
    }
}
