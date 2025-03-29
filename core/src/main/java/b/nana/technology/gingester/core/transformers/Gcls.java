package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.SchemaSupplier;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

@Names(1)
public final class Gcls implements Transformer<Object, Object> {

    private final SchemaGenerator schemaGenerator;
    private final TransformerFactory transformerFactory;

    public Gcls(Parameters parameters) {

        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .without(Option.SCHEMA_VERSION_INDICATOR)
                .with(Option.INLINE_ALL_SCHEMAS)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES);

        configBuilder.forTypesInGeneral().withPropertySorter((a, b) -> 0);

        configBuilder.forFields().withCustomDefinitionProvider((scope, context) -> {
                    SchemaSupplier schemaSupplierAnnotation = scope.getAnnotationConsideringFieldAndGetter(SchemaSupplier.class);
                    if (schemaSupplierAnnotation == null) return null;
                    try {
                        ObjectNode schema = schemaSupplierAnnotation.value().getConstructor().newInstance().get();
                        if (schema == null) return null;
                        return new CustomPropertyDefinition(schema);
                    } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });

        schemaGenerator = new SchemaGenerator(configBuilder.build());

        transformerFactory = parameters.providers == null ?
                TransformerFactory.withSpiProviders() :
                TransformerFactory.withProvidersByFqdn(parameters.providers);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        transform(context, out);
    }

    private <I, O> void transform(Context context, Receiver<Object> out) throws Exception {

        List<Class<? extends Transformer<?, ?>>> transformers = transformerFactory.getTransformers().toList();
        for (Class<? extends Transformer<?, ?>> transformer : transformers) {

            String name = transformerFactory.getUniqueName(transformer);

            //noinspection unchecked
            Optional<Class<?>> optParametersClass = transformerFactory
                    .getParameterRichConstructor((Class<? extends Transformer<I, O>>) transformer)
                    .map(constructor -> constructor.getParameterTypes()[0]);

            if (optParametersClass.isPresent()) {
                Class<?> parametersClass = optParametersClass.get();
                out.accept(context, new Output(name, schemaGenerator.generateSchema(parametersClass), parametersClass.getConstructor().newInstance()));
            } else {
                out.accept(context, new Output(name));
            }
        }
    }

    public static class Parameters {
        public List<String> providers;
    }

    public static class Output {

        public String name;
        public JsonNode schema;
        public Object defaultParameters;

        public Output(String name) {
            this(name, null, null);
        }

        public Output(String name, JsonNode schema, Object defaultParameters) {
            this.name = name;
            this.schema = schema;
            this.defaultParameters = defaultParameters;
        }
    }
}
