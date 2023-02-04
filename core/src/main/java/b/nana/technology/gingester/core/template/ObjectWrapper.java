package b.nana.technology.gingester.core.template;

import com.fasterxml.jackson.databind.JsonNode;
import freemarker.template.*;

import java.util.Iterator;
import java.util.Map;

import static b.nana.technology.gingester.core.template.FreemarkerTemplateFactory.JACKSON_WRAPPER;

final class ObjectWrapper extends DefaultObjectWrapper {

    ObjectWrapper(Version freemarkerVersion) {
        super(freemarkerVersion);
    }

    @Override
    public TemplateModel wrap(Object object) {
        if (object instanceof JsonNode) {
            return handleJsonNode((JsonNode) object);
        } else if (object instanceof Map) {
            return DefaultMapAdapter.adapt((Map<?, ?>) object, JACKSON_WRAPPER);
        } else {
            try {
                return super.wrap(object);
            } catch (TemplateModelException e) {
                throw new RuntimeException(e);  // TODO
            }
        }
    }

    private TemplateModel handleJsonNode(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            return new TemplateJsonObjectModel() {

                public String getAsString() {
                    return jsonNode.toString();
                }

                public TemplateModel get(String key) {
                    return handleJsonNode(jsonNode.path(key));
                }

                public boolean isEmpty() {
                    return jsonNode.isEmpty();
                }

                public KeyValuePairIterator keyValuePairIterator() {
                    Iterator<String> keys = jsonNode.fieldNames();
                    return new KeyValuePairIterator() {

                        public boolean hasNext() {
                            return keys.hasNext();
                        }

                        public KeyValuePair next() {
                            String key = keys.next();
                            return new KeyValuePair() {

                                public TemplateModel getKey() {
                                    return (TemplateScalarModel) () -> key;
                                }

                                public TemplateModel getValue() {
                                    return handleJsonNode(jsonNode.get(key));
                                }
                            };
                        }
                    };
                }

                public int size() {
                    return jsonNode.size();
                }

                public TemplateCollectionModel keys() {
                    Iterator<String> keys = jsonNode.fieldNames();
                    return () -> new TemplateModelIterator() {

                        public boolean hasNext() {
                            return keys.hasNext();
                        }

                        public TemplateModel next() {
                            String key = keys.next();  // ensuring call through
                            return (TemplateScalarModel) () -> key;
                        }
                    };
                }

                public TemplateCollectionModel values() {
                    Iterator<JsonNode> values = jsonNode.iterator();
                    return () -> new TemplateModelIterator() {

                        public boolean hasNext() {
                            return values.hasNext();
                        }

                        public TemplateModel next() {
                            return handleJsonNode(values.next());
                        }
                    };
                }
            };
        } else if (jsonNode.isArray()) {
            return new TemplateJsonArrayModel() {

                public String getAsString() {
                    return jsonNode.toString();
                }

                public TemplateModel get(int index) {
                    return handleJsonNode(jsonNode.path(index));
                }

                public int size() {
                    return jsonNode.size();
                }
            };
        } else if (jsonNode.isNumber()) {
            return (TemplateNumberModel) jsonNode::doubleValue;
        } else if (jsonNode.isBoolean()) {
            return (TemplateBooleanModel) jsonNode::booleanValue;
        } else if (jsonNode.isNull() || jsonNode.isMissingNode()) {
            return null;
        } else {
            return (TemplateScalarModel) jsonNode::asText;
        }
    }

    private interface TemplateJsonObjectModel extends TemplateHashModelEx2, TemplateScalarModel {

    }

    private interface TemplateJsonArrayModel extends TemplateSequenceModel, TemplateScalarModel {

    }
}
