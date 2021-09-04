package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.Transformer;
import b.nana.technology.gingester.core.provider.Provider;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class Configuration {

    private static final Set<Class<? extends Transformer<?, ?>>> TRANSFORMERS = ServiceLoader.load(Provider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .map(Provider::getTransformerClasses)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer(new Printer());

    public static Configuration fromJson(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "Configuration.fromJson called with null InputStream");
        return OBJECT_MAPPER.readValue(inputStream, Configuration.class);
    }

    public Boolean report;
    public List<TransformerConfiguration> transformers = new ArrayList<>();

    public String toJson() {
        try {
            return OBJECT_WRITER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void applyTo(Gingester gingester) {
        for (TransformerConfiguration transformerConfiguration : transformers) {

            List<Class<? extends Transformer<?, ?>>> transformerClasses = TRANSFORMERS.stream()
                    .filter(c -> c.getCanonicalName().endsWith(transformerConfiguration.transformer))
                    .collect(Collectors.toList());

            if (transformerClasses.isEmpty()) {
                throw new IllegalStateException("No transformer named " + transformerConfiguration.transformer);
            } else if (transformerClasses.size() > 1) {
                throw new IllegalStateException("Multiple transformers named " + transformerConfiguration.transformer);
            }

            Transformer<?, ?> transformer = instance(transformerClasses.get(0), transformerConfiguration.parameters);
            gingester.add(transformer, transformerConfiguration.controllerParameters);

            Class<? extends Transformer<?, ?>> transformerClass = transformerClasses.get(0);
        }
    }

    private Transformer<?, ?> instance(Class<? extends Transformer<?, ?>> transformerClass, JsonNode jsonParameters) {

        if (jsonParameters == null) {
            try {
                return transformerClass.getConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                // ignore, we'll instance the parameters class later on
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException("Calling parameter-less constructor on " + transformerClass.getCanonicalName() + " failed", e);
            }
        }

        Constructor<?> constructor = Arrays.stream(transformerClass.getConstructors())
                .filter(method -> method.getParameterCount() == 1 && method.getParameterTypes()[0].getSimpleName().equals("Parameters"))
                .reduce((a, b) -> { throw new IllegalStateException("Found multiple constructors accepting Parameters"); } )
                .orElseThrow(() -> new IllegalStateException("Did not find a constructor accepting Parameters on " + transformerClass.getCanonicalName()));

        Class<?> parameterClass = constructor.getParameterTypes()[0];

        Object parameters;
        if (jsonParameters != null) {
            try {
                parameters = Configuration.OBJECT_MAPPER.treeToValue(jsonParameters, parameterClass);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to map json parameters to " + parameterClass, e);
            }
        } else {
            try {
                parameters = parameterClass.getConstructor().newInstance();
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                throw new IllegalStateException("Calling parameter-less constructor on " + parameterClass.getCanonicalName() + " failed", e);
            }
        }

        try {
            return (Transformer<?, ?>) constructor.newInstance(parameters);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("Calling parameter-rich constructor on " + parameterClass.getCanonicalName() + " failed", e);
        }
    }

    private static class Printer extends DefaultPrettyPrinter {

        Printer() {
            _objectFieldValueSeparatorWithSpaces = ": ";
            _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
        }

        @Override
        public DefaultPrettyPrinter createInstance() {
            return new Printer();
        }
    }
}
