package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.flowbuilder.CliParser;
import b.nana.technology.gingester.core.flowbuilder.CliSplitter;
import b.nana.technology.gingester.core.provider.Provider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jodah.typetools.TypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// TODO instance instead of static and allow providers to be supplied, maybe through Gingester constructor to prevent lazy loading implementation

public final class TransformerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformerFactory.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature());

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

        // noinspection unchecked
        Class<? extends Transformer<I, O>> transformerClass = (Class<? extends Transformer<I, O>>) transformerClasses.get(0);
        return instance(transformerClass, jsonParameters);
    }

    public static <I, O> Transformer<I, O> instance(Class<? extends Transformer<I, O>> transformerClass, JsonNode jsonParameters) {

        if (transformerClass.getAnnotation(Deprecated.class) != null) {
            LOGGER.warn("Instancing deprecated transformer " + getUniqueName(transformerClass));
        }

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
                if (jsonParameters.isTextual()) {
                    throw new IllegalArgumentException("Failed to map parameters string to " + parameterClass, e);
                } else {
                    throw new IllegalArgumentException("Failed to map json parameters to " + parameterClass, e);
                }
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
            throw new IllegalStateException("Calling parameter-rich constructor on " + transformerClass.getCanonicalName() + " failed", e);
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
        // noinspection unchecked
        return getUniqueName((Class<? extends Transformer<?, ?>>) transformer.getClass());
    }

    public static String getUniqueName(Class<? extends Transformer<?, ?>> transformer) {
        if (transformer.getCanonicalName() == null) return "__anonymous__";
        int names = transformer.getAnnotation(Names.class) != null ? transformer.getAnnotation(Names.class).value() : 2;
        return getNames(transformer)
                .skip(names - 1)
                .filter(name -> getTransformersByName(name).skip(1).findFirst().isEmpty())
                .findFirst().orElseThrow(() -> new IllegalStateException("No unique name for " + transformer.getCanonicalName()));
    }

    private static Stream<Class<? extends Transformer<?, ?>>> getTransformersByName(String query) {
        return TRANSFORMERS.stream().filter(c -> getNames(c).anyMatch(query::equals));
    }

    private static Stream<String> getNames(Class<? extends Transformer<?, ?>> transformer) {
        String[] parts = transformer.getCanonicalName().split("\\.");
        StringBuilder nameBuilder = new StringBuilder();
        return IntStream.range(1, parts.length - 1)
                .mapToObj(i -> parts[parts.length - i])
                .filter(s -> !s.equalsIgnoreCase("transformers"))
                .map(s -> nameBuilder.insert(0, camelCase(s)).toString());
    }

    public static Stream<Class<? extends Transformer<?, ?>>> getTransformers() {
        return TRANSFORMERS.stream();
    }

    public static <I, O> Stream<String> getTransformerHelps() {
        return TRANSFORMERS.stream()
                .filter(c -> c.getAnnotation(Deprecated.class) == null)
                .map(transformer -> {

                    // TODO indenting is now spread out over the Main class and here, would be nice to consolidate

                    String uniqueName = getUniqueName(transformer);
                    StringBuilder help = new StringBuilder(uniqueName);

                    // noinspection unchecked
                    getParameterRichConstructor((Class<? extends Transformer<I, O>>) transformer).ifPresent(constructor -> {
                        Class<?> parametersClass = constructor.getParameterTypes()[0];
                        try {
                            Object parameters = parametersClass.getConstructor().newInstance();
                            String defaultParameters = OBJECT_MAPPER.writeValueAsString(parameters);
                            help.append(" '");
                            help.append(defaultParameters);
                            help.append('\'');
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    });

                    Optional.ofNullable(transformer.getAnnotation(Description.class))
                            .map(description -> "\n        " + description.value())
                            .ifPresent(help::append);

                    Arrays.stream(transformer.getAnnotationsByType(Example.class))
                            .map(example -> {

                                StringBuilder stringBuilder = new StringBuilder("\n");

                                boolean checkFailed = checkExample(uniqueName, example).isPresent();
                                if (checkFailed) {
                                    stringBuilder.append("broken: e.g. ");
                                } else {
                                    stringBuilder.append("        e.g. ");
                                }

                                stringBuilder.append(uniqueName);

                                if (!example.example().isEmpty()) {
                                    stringBuilder
                                            .append(' ')
                                            .append(example.example());
                                }

                                if (!example.description().isEmpty()) {
                                    stringBuilder
                                            .append("  # ")
                                            .append(example.description());
                                }

                                return stringBuilder.toString();

                            })
                            .forEach(help::append);

                    return help.toString();
                })
                .sorted();
    }

    private static <I, O> Optional<? extends Constructor<? extends Transformer<I, O>>> getParameterRichConstructor(Class<? extends Transformer<I, O>> transformerClass) {
        // noinspection unchecked
        return Arrays.stream(transformerClass.getConstructors())
                .map(c -> (Constructor<? extends Transformer<I, O>>) c)
                .filter(method -> method.getParameterCount() == 1 && method.getParameterTypes()[0].getSimpleName().equals("Parameters"))
                .reduce((a, b) -> { throw new IllegalStateException("Found multiple constructors accepting Parameters"); } );
    }

    private static String camelCase(String name) {
        return CASE_HINTS.getOrDefault(name, Character.toUpperCase(name.charAt(0)) + name.substring(1));
    }



    public static Optional<CheckExampleException> checkExample(Class<? extends Transformer<?, ?>> transformer, Example example) {
        return checkExample(getUniqueName(transformer), example);
    }

    public static Optional<CheckExampleException> checkExample(String uniqueName, Example example) {
        if (example.test()) {
            String cli = getExampleCli(uniqueName, example);
            try {
                CliParser.parse(new Gingester(), CliSplitter.split("-t " + uniqueName + " " + example.example()));
            } catch (Exception e) {
                return Optional.of(new CheckExampleException(cli, e));
            }
        }
        return Optional.empty();
    }

    private static String getExampleCli(String uniqueName, Example example) {
        if (!example.example().isEmpty()) {
            return "-t " + uniqueName + " " + example.example();
        } else {
            return "-t " + uniqueName;
        }
    }

    public static class CheckExampleException extends Exception {
        public CheckExampleException(String cli, Throwable cause) {
            super("Example failed: " + cli, cause);
        }
    }
}
