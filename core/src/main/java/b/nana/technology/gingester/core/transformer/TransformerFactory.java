package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.provider.Provider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jodah.typetools.TypeResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class TransformerFactory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    private static final Set<Class<? extends Transformer<?, ?>>> TRANSFORMERS = ServiceLoader.load(Provider.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .map(Provider::getTransformerClasses)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    private static final Set<Class<? extends Transformer<?, ?>>> PURE_TRANSFORMERS = TRANSFORMERS
            .stream()
            .filter(c -> c.getAnnotation(Pure.class) != null)
            .collect(Collectors.toSet());

    private static final Map<String, String> CASE_HINTS = ServiceLoader.load(Provider.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .map(Provider::getCaseHints)
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                    (a, b) -> {
                        if (!a.equals(b)) throw new IllegalStateException("Conflicting case hints: " + a + " and " + b);
                        return a;
                    }));

    private TransformerFactory() {}

    public static <I, O> Transformer<I, O> instance(String name) {
        return instance(name, null);
    }

    public static <I, O> Transformer<I, O> instance(String name, JsonNode jsonParameters) {

        List<Class<? extends Transformer<?, ?>>> transformerClasses =
                getTransformersByName(name).collect(Collectors.toList());

        if (transformerClasses.isEmpty()) {
            throw new IllegalArgumentException("No transformer named " + name);
        } else if (transformerClasses.size() > 1) {
            String uniqueNames = transformerClasses.stream()
                    .map(TransformerFactory::getUniqueName)
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(String.format(
                    "Multiple transformers named %s: %s",
                    name,
                    uniqueNames
            ));
        }

        Class<? extends Transformer<I, O>> transformerClass = (Class<? extends Transformer<I, O>>) transformerClasses.get(0);
        return instance(transformerClass, jsonParameters);
    }

    public static <I, O> Transformer<I, O> instance(Class<? extends Transformer<I, O>> transformerClass, JsonNode jsonParameters) {

        if (jsonParameters == null) {
            try {
                return transformerClass.getConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                // ignore, we'll instance the parameters class later on
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException("Calling parameter-less constructor on " + transformerClass.getCanonicalName() + " failed", e);
            }
        }

        Constructor<? extends Transformer<I, O>> constructor = getParameterRichConstructor(transformerClass)
                .orElseThrow(() -> new IllegalStateException("Did not find a constructor accepting Parameters on " + getUniqueName(transformerClass)));

        Class<?> parameterClass = constructor.getParameterTypes()[0];

        Object parameters;
        if (jsonParameters != null) {
            try {
                parameters = OBJECT_MAPPER.treeToValue(jsonParameters, parameterClass);
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
            return constructor.newInstance(parameters);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("Calling parameter-rich constructor on " + parameterClass.getCanonicalName() + " failed", e);
        }
    }

    public static <I, O> Optional<ArrayDeque<Class<? extends Transformer<?, ?>>>> getBridge(Class<I> from, Class<O> to) {

        List<ArrayDeque<Class<? extends Transformer<?, ?>>>> options = PURE_TRANSFORMERS.stream()
                .filter(c -> TypeResolver.resolveRawArguments(Transformer.class, c)[0].isAssignableFrom(from))
                .map(c -> new ArrayDeque<Class<? extends Transformer<?, ?>>>(Collections.singleton(c)))
                .collect(Collectors.toList());

        while (!options.isEmpty()) {

            Optional<ArrayDeque<Class<? extends Transformer<?, ?>>>> bridge = options.stream()
                    .filter(b -> to.isAssignableFrom(TypeResolver.resolveRawArguments(Transformer.class, b.getLast())[1]))
                    .findFirst();

            if (bridge.isPresent()) {
                return bridge;
            } else {
                options = options.stream()
                        .flatMap(b -> {
                            Class<?> tailType = TypeResolver.resolveRawArguments(Transformer.class, b.getLast())[1];
                            return PURE_TRANSFORMERS.stream()
                                    .filter(c -> !b.contains(c))
                                    .filter(c -> TypeResolver.resolveRawArguments(Transformer.class, c)[0].isAssignableFrom(tailType))
                                    .map(c -> {
                                        ArrayDeque<Class<? extends Transformer<?, ?>>> next = new ArrayDeque<>(b);
                                        next.add(c);
                                        return next;
                                    });
                        })
                        .collect(Collectors.toList());
            }
        }

        return Optional.empty();
    }

    public static String getUniqueName(Transformer<?, ?> transformer) {
        return getUniqueName((Class<? extends Transformer<?, ?>>) transformer.getClass());
    }

    public static String getUniqueName(Class<? extends Transformer<?, ?>> transformer) {
        return getNames(transformer)
                .filter(name -> getTransformersByName(name).skip(1).findFirst().isEmpty())
                .findFirst().orElseThrow(() -> new IllegalStateException("No unique name for " + transformer.getCanonicalName()));
    }

    private static Stream<Class<? extends Transformer<?, ?>>> getTransformersByName(String query) {
        return TRANSFORMERS.stream().filter(c -> getNames(c).anyMatch(name -> name.endsWith(query)));
    }

    private static Stream<String> getNames(Class<? extends Transformer<?, ?>> transformer) {
        int[] nameFormat = transformer.getAnnotation(Names.class) != null ? transformer.getAnnotation(Names.class).value() : new int[] { 2, 1 };
        if (nameFormat.length == 0) throw new IllegalArgumentException("Empty @Name on " + transformer.getCanonicalName());
        String[] parts = transformer.getCanonicalName().split("\\.");
        String name = Arrays.stream(nameFormat).mapToObj(i -> camelCase(parts[parts.length - i])).collect(Collectors.joining());
        StringBuilder builder = new StringBuilder(name);
        return Stream.concat(Stream.of(name), IntStream.of(Arrays.stream(nameFormat).max().orElseThrow() + 1, parts.length - 1)
                .mapToObj(i -> parts[parts.length - i])
                .filter(s -> !s.equalsIgnoreCase("transformers"))
                .map(s -> builder.insert(0, camelCase(s)).toString()));
    }

    public static <I, O> Stream<String> getTransformerHelps() {
        return TRANSFORMERS.stream()
                .map(transformer -> {
                    StringBuilder help = new StringBuilder(getUniqueName(transformer));
                    getParameterRichConstructor((Class<? extends Transformer<I, O>>) transformer).ifPresent(constructor -> {
                        Class<?> parametersClass = constructor.getParameterTypes()[0];
                        try {
                            Object parameters = parametersClass.getConstructor().newInstance();
                            help.append(' ');
                            help.append(OBJECT_MAPPER.writeValueAsString(parameters));
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    });
                    return help.toString();
                })
                .sorted();
    }

    private static <I, O> Optional<? extends Constructor<? extends Transformer<I, O>>> getParameterRichConstructor(Class<? extends Transformer<I, O>> transformerClass) {
        return Arrays.stream(transformerClass.getConstructors())
                .map(c -> (Constructor<? extends Transformer<I, O>>) c)
                .filter(method -> method.getParameterCount() == 1 && method.getParameterTypes()[0].getSimpleName().equals("Parameters"))
                .reduce((a, b) -> { throw new IllegalStateException("Found multiple constructors accepting Parameters"); } );
    }

    private static String camelCase(String name) {
        return CASE_HINTS.getOrDefault(name, Character.toUpperCase(name.charAt(0)) + name.substring(1));
    }
}
