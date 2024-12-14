package b.nana.technology.gingester.core.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import freemarker.template.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class ObjectWrapper extends DefaultObjectWrapper {

    ObjectWrapper(Version freemarkerVersion) {
        super(freemarkerVersion);
        setExposeFields(true);
    }

    public TemplateModel wrap(Object object) throws TemplateModelException {
        if (object instanceof JsonNode jsonNode) {
            return handleJsonNode(jsonNode);
        } else if (object instanceof List<?> list) {
            return handleListNode(list);
        } else if (object instanceof Map<?, ?> map) {
            return handleMapNode(map);
        } else if (object instanceof Collection<?> collection) {
            return handleCollectionNode(collection);
        } else {
            return super.wrap(object);
        }
    }

    private TemplateModel handleJsonNode(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            return new JsonObjectModel() {

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

                public TemplateModel getAPI() throws TemplateModelException {
                    return wrapAsAPI(jsonNode);
                }
            };
        } else if (jsonNode.isArray()) {
            return new JsonArrayModel() {

                public String getAsString() {
                    return jsonNode.toString();
                }

                public TemplateModel get(int index) {
                    return handleJsonNode(jsonNode.path(index));
                }

                public int size() {
                    return jsonNode.size();
                }

                public TemplateModel getAPI() throws TemplateModelException {
                    return wrapAsAPI(jsonNode);
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

    private TemplateModel handleMapNode(Map<?, ?> map) {
        return new MapModel() {

            public String getAsString() throws TemplateModelException {
                try {
                    return FreemarkerTemplateFactory.OBJECT_MAPPER.writeValueAsString(map);
                } catch (JsonProcessingException e) {
                    throw new TemplateModelException(e);
                }
            }

            public TemplateModel get(String key) throws TemplateModelException {
                return wrap(map.get(key));
            }

            public boolean isEmpty() {
                return map.isEmpty();
            }

            public int size() {
                return map.size();
            }

            public TemplateCollectionModel keys() {
                return new SimpleCollection(map.keySet(), ObjectWrapper.this);
            }

            public TemplateCollectionModel values() {
                return new SimpleCollection(map.values(), ObjectWrapper.this);
            }

            public KeyValuePairIterator keyValuePairIterator() {
                return new MapKeyValuePairIterator(map, ObjectWrapper.this);
            }

            public TemplateModel getAPI() throws TemplateModelException {
                return wrapAsAPI(map);
            }
        };
    }

    private TemplateModel handleListNode(List<?> list) {
        return new ListModel() {

            public String getAsString() throws TemplateModelException {
                try {
                    return FreemarkerTemplateFactory.OBJECT_MAPPER.writeValueAsString(list);
                } catch (JsonProcessingException e) {
                    throw new TemplateModelException(e);
                }
            }

            public TemplateModel get(int i) throws TemplateModelException {
                return wrap(list.get(i));
            }

            public int size() {
                return list.size();
            }

            public TemplateModel getAPI() throws TemplateModelException {
                return wrapAsAPI(list);
            }
        };
    }

    private TemplateModel handleCollectionNode(Collection<?> collection) {
        return new CollectionModel() {

            public String getAsString() throws TemplateModelException {
                try {
                    return FreemarkerTemplateFactory.OBJECT_MAPPER.writeValueAsString(collection);
                } catch (JsonProcessingException e) {
                    throw new TemplateModelException(e);
                }
            }

            public TemplateModelIterator iterator() throws TemplateModelException {
                Iterator<?> iterator = collection.iterator();
                return new TemplateModelIterator() {

                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public TemplateModel next() throws TemplateModelException {
                        return wrap(iterator.next());
                    }
                };
            }

            public int size() {
                return collection.size();
            }

            public boolean isEmpty() {
                return collection.isEmpty();
            }

            public TemplateModel getAPI() throws TemplateModelException {
                return wrapAsAPI(collection);
            }
        };
    }

    private interface JsonObjectModel extends TemplateHashModelEx2, TemplateScalarModel, TemplateModelWithAPISupport {

    }

    private interface JsonArrayModel extends TemplateSequenceModel, TemplateScalarModel, TemplateModelWithAPISupport {

    }

    private interface MapModel extends TemplateHashModelEx2, TemplateScalarModel, TemplateModelWithAPISupport {

    }

    private interface ListModel extends TemplateSequenceModel, TemplateScalarModel, TemplateModelWithAPISupport {

    }

    private interface CollectionModel extends TemplateCollectionModelEx, TemplateScalarModel, TemplateModelWithAPISupport {

    }
}
