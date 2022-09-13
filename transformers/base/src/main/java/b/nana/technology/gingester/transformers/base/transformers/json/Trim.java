package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public final class Trim implements Transformer<JsonNode, JsonNode> {

    private final boolean emptyArrays;
    private final boolean emptyObjects;
    private final boolean emptyValues;
    private final boolean keyWhitespace;
    private final boolean valueWhitespace;
    private final boolean nulls;

    public Trim(Parameters parameters) {
        emptyArrays = parameters.emptyArrays;
        emptyObjects = parameters.emptyObjects;
        emptyValues = parameters.emptyValues;
        keyWhitespace = parameters.keyWhitespace;
        valueWhitespace = parameters.valueWhitespace;
        nulls = parameters.nulls;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws Exception {
        trim(in);
        out.accept(context, in);
    }

    private void trim(JsonNode node) {

        if (node.isObject()) {

            ObjectNode objectNode = (ObjectNode) node;
            List<String> keys = new ArrayList<>(objectNode.size());
            objectNode.fieldNames().forEachRemaining(keys::add);

            for (String key : keys) {
                JsonNode value = objectNode.get(key);

                if (value.isObject()) {
                    trim(value);
                    if (emptyObjects && value.isEmpty()) {
                        objectNode.remove(key);
                        continue;
                    }
                } else if (value.isArray()) {
                    trim(value);
                    if (emptyArrays && value.isEmpty()) {
                        objectNode.remove(key);
                        continue;
                    }
                } else if (nulls && value.isNull()) {
                    objectNode.remove(key);
                    continue;
                } else if ((valueWhitespace || emptyValues) && value.isTextual()) {
                    if (valueWhitespace) {
                        String text = value.asText();
                        String trimmed = text.trim();
                        if (emptyValues && trimmed.isEmpty()) {
                            objectNode.remove(key);
                            continue;
                        } else if (trimmed.length() != text.length()) {
                            objectNode.set(key, JsonNodeFactory.instance.textNode(trimmed));
                        }
                    } else if (value.asText().isEmpty()) {
                        objectNode.remove(key);
                        continue;
                    }
                }

                if (keyWhitespace) {
                    String trimmed = key.trim();
                    if (trimmed.length() != key.length()) {
                        objectNode.set(trimmed, objectNode.remove(key));
                    }
                }
            }

        } else if (node.isArray()) {

            ArrayNode arrayNode = (ArrayNode) node;

            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode value = arrayNode.get(i);

                if (value.isObject()) {
                    trim(value);
                    if (emptyObjects && value.isEmpty()) {
                        arrayNode.remove(i--);
                    }
                } else if (value.isArray()) {
                    trim(value);
                    if (emptyArrays && value.isEmpty()) {
                        arrayNode.remove(i--);
                    }
                } else if (nulls && value.isNull()) {
                    arrayNode.remove(i--);
                } else if ((valueWhitespace || emptyValues) && value.isTextual()) {
                    if (valueWhitespace) {
                        String text = value.asText();
                        String trimmed = text.trim();
                        if (emptyValues && trimmed.isEmpty()) {
                            arrayNode.remove(i--);
                        } else if (trimmed.length() != text.length()) {
                            arrayNode.set(i, JsonNodeFactory.instance.textNode(trimmed));
                        }
                    } else if (value.asText().isEmpty()) {
                        arrayNode.remove(i--);
                    }
                }
            }
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, NormalizingDeserializer::a);
                rule(JsonNode::isArray, array -> {
                    boolean emptyArrays = false;
                    boolean emptyObjects = false;
                    boolean emptyValues = false;
                    boolean keyWhitespace = false;
                    boolean valueWhitespace = false;
                    boolean nulls = false;
                    for (JsonNode jsonNode : array) {
                        if (jsonNode.isTextual()) {
                            switch (jsonNode.asText()) {
                                case "emptyArrays": emptyArrays = true; continue;
                                case "emptyObjects": emptyObjects = true; continue;
                                case "emptyValues": emptyValues = true; continue;
                                case "keyWhitespace": keyWhitespace = true; continue;
                                case "valueWhitespace": valueWhitespace = true; continue;
                                case "nulls": nulls = true; continue;
                            }
                        }
                        throw new IllegalArgumentException("JsonTrim parameter parsing failed at " + jsonNode);
                    }
                    return o(
                        "emptyArrays", emptyArrays,
                        "emptyObjects", emptyObjects,
                        "emptyValues", emptyValues,
                        "keyWhitespace", keyWhitespace,
                        "valueWhitespace", valueWhitespace,
                        "nulls", nulls
                    );
                });
            }
        }

        public boolean emptyArrays = true;
        public boolean emptyObjects = true;
        public boolean emptyValues = true;
        public boolean keyWhitespace = true;
        public boolean valueWhitespace = true;
        public boolean nulls = true;
    }
}
