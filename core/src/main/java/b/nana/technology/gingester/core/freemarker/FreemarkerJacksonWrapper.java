package b.nana.technology.gingester.core.freemarker;

import com.fasterxml.jackson.databind.JsonNode;
import freemarker.template.*;

import java.util.Iterator;

public class FreemarkerJacksonWrapper extends DefaultObjectWrapper {

    public FreemarkerJacksonWrapper(Version freemarkerVersion) {
        super(freemarkerVersion);
    }

    @Override
    protected TemplateModel handleUnknownType(Object object) throws TemplateModelException {
        if (object instanceof JsonNode) {
            return handleJsonNode((JsonNode) object);
        } else {
            return super.handleUnknownType(object);
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

    public interface TemplateJsonObjectModel extends TemplateHashModelEx2, TemplateScalarModel {

    }

    public interface TemplateJsonArrayModel extends TemplateSequenceModel, TemplateScalarModel {

    }
}
