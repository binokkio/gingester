package b.nana.technology.gingester.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class Provider {

    private static final List<Resolver> RESOLVERS = ServiceLoader.load(Resolver.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toList());

    static Optional<String> name(Transformer<?, ?> transformer) {

        String canonicalName = transformer.getClass().getCanonicalName();
        if (canonicalName == null) return Optional.empty();

        List<String> names = RESOLVERS.stream()
                .map(r -> r.name(transformer))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (names.isEmpty()) throw new NoSuchElementException("No name for " + transformer.getClass().getCanonicalName());
        else if (names.size() > 1) throw new IllegalStateException("Multiple names for " + transformer.getClass().getCanonicalName());

        return Optional.of(names.get(0));
    }

    static Transformer<?, ?> instance(String name, JsonNode jsonParameters) {

        List<Class<Transformer<?, ?>>> transformerClasses = RESOLVERS.stream()
                .map(resolver -> resolver.resolve(name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (transformerClasses.isEmpty()) throw new NoSuchElementException("No transformer named " + name);
        else if (transformerClasses.size() > 1) throw new IllegalStateException("Multiple transformers named " + name);

        Class<Transformer<?, ?>> transformerClass = transformerClasses.get(0);

        if (jsonParameters != null) {

            Class<?> parameterClass;
            try {
                parameterClass = Class.forName(transformerClass.getCanonicalName() + "$Parameters");
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Parameters given but no inner static class Parameters found on " + transformerClass);
            }

            Object parameters;
            try {
                parameters = Configuration.OBJECT_MAPPER.treeToValue(jsonParameters, parameterClass);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to map json parameters to " + parameterClass, e);
            }

            try {
                Transformer<?, ?> transformer = transformerClass.getConstructor(parameterClass).newInstance(parameters);
                if (transformer.parameters != parameters) {
                    // TODO maybe log a warning
                }
                return transformer;
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Public constructor accepting " + parameterClass + " missing on " + transformerClass);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);  // TODO
            }

        } else {

            try {
                return transformerClass.getConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Public parameter-less constructor missing on " + transformerClass);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);  // TODO
            }
        }
    }
}
