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
    private final boolean emptyText;
    private final boolean keyWhitespace;
    private final boolean valueWhitespace;
    private final boolean nulls;

    public Trim(Parameters parameters) {
        emptyArrays = parameters.emptyArrays;
        emptyObjects = parameters.emptyObjects;
        emptyText = parameters.emptyText;
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
                        if (nulls) {
                            objectNode.remove(key);
                            continue;
                        } else {
                            objectNode.replace(key, JsonNodeFactory.instance.nullNode());
                        }
                    }
                } else if (value.isArray()) {
                    trim(value);
                    if (emptyArrays && value.isEmpty()) {
                        if (nulls) {
                            objectNode.remove(key);
                            continue;
                        } else {
                            objectNode.replace(key, JsonNodeFactory.instance.nullNode());
                        }
                    }
                } else if (nulls && value.isNull()) {
                    objectNode.remove(key);
                    continue;
                } else if ((valueWhitespace || emptyText) && value.isTextual()) {
                    if (valueWhitespace) {
                        String text = value.asText();
                        String trimmed = text.trim();
                        if (emptyText && trimmed.isEmpty()) {
                            if (nulls) {
                                objectNode.remove(key);
                                continue;
                            } else {
                                objectNode.replace(key, JsonNodeFactory.instance.nullNode());
                            }
                        } else if (trimmed.length() != text.length()) {
                            objectNode.set(key, JsonNodeFactory.instance.textNode(trimmed));
                        }
                    } else if (value.asText().isEmpty()) {
                        if (nulls) {
                            objectNode.remove(key);
                            continue;
                        } else {
                            objectNode.replace(key, JsonNodeFactory.instance.nullNode());
                        }
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
                        if (nulls)
                            arrayNode.remove(i--);
                        else
                            arrayNode.set(i, JsonNodeFactory.instance.nullNode());
                    }
                } else if (value.isArray()) {
                    trim(value);
                    if (emptyArrays && value.isEmpty()) {
                        if (nulls)
                            arrayNode.remove(i--);
                        else
                            arrayNode.set(i, JsonNodeFactory.instance.nullNode());
                    }
                } else if (nulls && value.isNull()) {
                    arrayNode.remove(i--);
                } else if ((valueWhitespace || emptyText) && value.isTextual()) {
                    if (valueWhitespace) {
                        String text = value.asText();
                        String trimmed = text.trim();
                        if (emptyText && trimmed.isEmpty()) {
                            if (nulls)
                                arrayNode.remove(i--);
                            else
                                arrayNode.set(i, JsonNodeFactory.instance.nullNode());
                        } else if (trimmed.length() != text.length()) {
                            arrayNode.set(i, JsonNodeFactory.instance.textNode(trimmed));
                        }
                    } else if (value.asText().isEmpty()) {
                        if (nulls)
                            arrayNode.remove(i--);
                        else
                            arrayNode.set(i, JsonNodeFactory.instance.nullNode());
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
                rule(JsonNode::isArray, array -> flags(array, 0, o()));
            }
        }

        public boolean emptyArrays = true;
        public boolean emptyObjects = true;
        public boolean emptyText = true;
        public boolean keyWhitespace = true;
        public boolean valueWhitespace = true;
        public boolean nulls = true;
    }
}
