package b.nana.technology.gingester.transformers.freemarker.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import freemarker.template.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Freemarker extends Transformer<Object, String> {

    private static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_31;

    private final Template template;

    public Freemarker(Parameters parameters) {
        super(parameters);

        Configuration configuration = new Configuration(FREEMARKER_VERSION);
        configuration.setObjectWrapper(new ObjectWrapper());

        try {
            template = new Template("Template", getTemplateString(parameters.template, parameters.interpretation), configuration);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize template", e);
        }
    }

    private String getTemplateString(String template, TemplateParameterInterpretation interpretation) {

        Configuration configuration = new Configuration(FREEMARKER_VERSION);
        configuration.setObjectWrapper(new ObjectWrapper());

        switch (interpretation) {

            case FILE: return readTemplateFile(template).orElseThrow();
            case RESOURCE: return readTemplateResource(template).orElseThrow();

            case AUTO:
                return Stream.of(
                                () -> readTemplateResource(template),
                                (Supplier<Optional<String>>) () -> readTemplateFile(template))
                        .map(Supplier::get)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst().orElseThrow();

            default:
                throw new IllegalStateException("No case for " + interpretation);
        }
    }

    @Override
    protected void transform(Context context, Object input) throws TemplateException, IOException {
        System.out.println(input);
        StringWriter stringWriter = new StringWriter();
        template.process(input, stringWriter);
        emit(context, stringWriter.toString());
    }


    public static class Parameters {

        public String template;
        public TemplateParameterInterpretation interpretation = TemplateParameterInterpretation.AUTO;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String template) {
            this.template = template;
        }
    }

    public enum TemplateParameterInterpretation {
        AUTO,
        FILE,
        RESOURCE
    }


    private static class ObjectWrapper extends DefaultObjectWrapper {

        private ObjectWrapper() {
            super(FREEMARKER_VERSION);
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
                return new TemplateHashModelEx2() {

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
                return new TemplateSequenceModel() {
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
                return TemplateModel.NOTHING;
            } else {
                return (TemplateScalarModel) jsonNode::asText;
            }
        }
    }


    private static Optional<String> readTemplateFile(String template) {
        Path path = Paths.get(template);
        if (Files.exists(path)) return Optional.empty();
        try {
            return Optional.of(Files.readString(Paths.get(template)));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read template file", e);
        }
    }

    private static Optional<String> readTemplateResource(String template) {
        InputStream inputStream = Freemarker.class.getResourceAsStream(template);
        if (inputStream == null) return Optional.empty();
        try {
            return Optional.of(new String(inputStream.readAllBytes()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read template resource", e);
        }
    }
}
