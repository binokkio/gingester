package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.configuration.Parameters;
import b.nana.technology.gingester.core.provider.Provider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public final class TransformerFactory {

    private static final Set<Class<? extends Transformer<?, ?>>> TRANSFORMERS = ServiceLoader.load(Provider.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .map(Provider::getTransformerClasses)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TransformerFactory() {}

    public static <I, O> Transformer<I, O> instance(Parameters parameters) {

        List<Class<? extends Transformer<?, ?>>> transformerClasses = TRANSFORMERS.stream()
                .filter(c -> c.getCanonicalName().toLowerCase(Locale.ENGLISH).endsWith(parameters.getTransformer().toLowerCase(Locale.ENGLISH)))
                .collect(Collectors.toList());

        if (transformerClasses.isEmpty()) {
            throw new IllegalStateException("No transformer named " + parameters.getTransformer());
        } else if (transformerClasses.size() > 1) {
            throw new IllegalStateException("Multiple transformers named " + parameters.getTransformer());
        }

        Class<? extends Transformer<I, O>> transformerClass = (Class<? extends Transformer<I, O>>) transformerClasses.get(0);

        if (parameters.getParameters() == null) {
            try {
                return transformerClass.getConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                // ignore, we'll instance the parameters class later on
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException("Calling parameter-less constructor on " + transformerClass.getCanonicalName() + " failed", e);
            }
        }

        Constructor<? extends Transformer<I, O>> constructor = Arrays.stream(transformerClass.getConstructors())
                .map(c -> (Constructor<? extends Transformer<I, O>>) c)
                .filter(method -> method.getParameterCount() == 1 && method.getParameterTypes()[0].getSimpleName().equals("Parameters"))
                .reduce((a, b) -> { throw new IllegalStateException("Found multiple constructors accepting Parameters"); } )
                .orElseThrow(() -> new IllegalStateException("Did not find a constructor accepting Parameters on " + transformerClass.getCanonicalName()));

        Class<?> parameterClass = constructor.getParameterTypes()[0];

        Object params;
        if (parameters.getParameters() != null) {
            try {
                params = OBJECT_MAPPER.treeToValue(parameters.getParameters(), parameterClass);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to map json parameters to " + parameterClass, e);
            }
        } else {
            try {
                params = parameterClass.getConstructor().newInstance();
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                throw new IllegalStateException("Calling parameter-less constructor on " + parameterClass.getCanonicalName() + " failed", e);
            }
        }

        try {
            return constructor.newInstance(params);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("Calling parameter-rich constructor on " + parameterClass.getCanonicalName() + " failed", e);
        }
    }
}
