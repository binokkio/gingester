package b.nana.technology.gingester.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

final class Provider {

    private static final List<Resolver> RESOLVERS = ServiceLoader.load(Resolver.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toList());

    static String name(Transformer<?, ?> transformer) {

        String canonicalName = transformer.getClass().getCanonicalName();
        if (canonicalName == null) return "UnknownTransformer";

        return RESOLVERS.stream()
                .map(r -> r.name(transformer))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce((a, b) -> { throw new IllegalStateException("Multiple names for " + canonicalName); })
                .orElseThrow(() -> new NoSuchElementException("No name for " + canonicalName));
    }

    static Transformer<?, ?> instance(String name, JsonNode jsonParameters) {

        Class<Transformer<?, ?>> transformerClass = RESOLVERS.stream()
                .map(resolver -> resolver.resolve(name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce((a, b) -> { throw new IllegalStateException("Multiple transformers named " + name); })
                .orElseThrow(() -> new NoSuchElementException("No transformer named " + name));

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
                .orElseThrow(() -> new IllegalStateException("Did not find a constructor accepting Parameters"));

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
            Transformer<?, ?> transformer = (Transformer<?, ?>) constructor.newInstance(parameters);
            if (transformer.parameters != parameters) {
                // TODO maybe log a warning
            }
            return transformer;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);  // TODO
        }
    }
}
