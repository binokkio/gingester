package b.nana.technology.gingester.core.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import freemarker.template.*;

import java.util.Iterator;
import java.util.Map;

final class ObjectWrapper extends DefaultObjectWrapper {

    ObjectWrapper(Version freemarkerVersion) {
        super(freemarkerVersion);
    }

    @Override
    public TemplateModel wrap(Object object) throws TemplateModelException {
        if (object instanceof Map) {
            return handleMapNode((Map<?, ?>) object);
        } else if (object instanceof JsonNode) {
            return handleJsonNode((JsonNode) object);
        } else {
            try {
                return super.wrap(object);
            } catch (TemplateModelException e) {
                throw new TemplateModelException(e);
            }
        }
    }

    private TemplateModel handleMapNode(Map<?, ?> map) {
        return new TemplateMapModel() {

            @Override
            public String getAsString() throws TemplateModelException {
                try {
                    return FreemarkerTemplateFactory.OBJECT_MAPPER.writeValueAsString(map);
                } catch (JsonProcessingException e) {
                    throw new TemplateModelException(e);
                }
            }

            @Override
            public TemplateModel get(String key) throws TemplateModelException {
                return wrap(map.get(key));
            }

            @Override
            public boolean isEmpty() {
                return map.isEmpty();
            }

            @Override
            public int size() {
                return map.size();
            }

            @Override
            public TemplateCollectionModel keys() {
                return new SimpleCollection(map.keySet(), ObjectWrapper.this);
            }

            @Override
            public TemplateCollectionModel values() {
                return new SimpleCollection(map.values(), ObjectWrapper.this);
            }

            @Override
            public KeyValuePairIterator keyValuePairIterator() {
                return new MapKeyValuePairIterator(map, ObjectWrapper.this);
            }
        };
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

    private interface TemplateMapModel extends TemplateHashModelEx2, TemplateScalarModel {

    }

    private interface TemplateJsonObjectModel extends TemplateHashModelEx2, TemplateScalarModel {

    }

    private interface TemplateJsonArrayModel extends TemplateSequenceModel, TemplateScalarModel {

    }
}
