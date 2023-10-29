package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Experimental;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Experimental
@Example(example = ", a b", description = "Split input on ',' and map the values in order to keys 'a' and 'b'")
@Example(example = ", a! b", description = "Same as above but accumulating in 'a' if split produces too many values")
@Example(example = "'{delimiter: \",\", keys: [\"a\", \"b\"], accumulator: \"a\"}'", description = "Same as above")
@Example(example = ", a b", description = "Same as above but defaulting to accumulate in the last key, in this case 'b'")
public final class SplitMap implements Transformer<String, Map<String, String>> {

    private final char delimiter;
    private final List<String> keys;
    private final int accumulator;

    public SplitMap(Parameters parameters) {
        delimiter = parameters.delimiter;
        keys = parameters.keys;

        if (parameters.accumulator == null) {
            accumulator = keys.size() - 1;
        } else {
            accumulator = keys.indexOf(parameters.accumulator);
            if (accumulator == -1) {
                throw new IllegalArgumentException("Accumulator not present in keys");
            }
        }
    }

    @Override
    public void transform(Context context, String in, Receiver<Map<String, String>> out) {

        Map<String, String> result = new HashMap<>();
        int leftKey = 0;

        int left = 0;
        for (int i = 0; i < in.length(); i++) {
            if (leftKey == accumulator) break;
            if (in.charAt(i) == delimiter) {
                result.put(keys.get(leftKey++), in.substring(left, i));
                left = i + 1;
            }
        }

        int rightKey = keys.size() - 1;

        int right = in.length() - 1;
        for (int i = in.length() - 1; i >= 0; i--) {
            if (rightKey == accumulator) break;
            if (in.charAt(i) == delimiter) {
                result.put(keys.get(rightKey--), in.substring(i + 1, right + 1));
                right = i - 1;
            }
        }

        if (leftKey != rightKey || left > right + 1)
            throw new IllegalStateException("Split produced less values than keys");

        result.put(keys.get(accumulator), in.substring(left, right + 1));

        out.accept(context, result);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isArray, array -> {
                    ArrayNode keys = a();
                    String accumulator = null;
                    for (int i = 1; i < array.size(); i++) {
                        String item = array.get(i).asText();
                        if (item.endsWith("!")) {
                            accumulator = item.substring(0, item.length() - 1);
                            keys.add(accumulator);
                        } else {
                            keys.add(item);
                        }
                    }
                    return o("delimiter", array.get(0), "keys", keys, "accumulator", accumulator);
                });
            }
        }

        public char delimiter;
        public List<String> keys;
        public String accumulator;
    }
}
