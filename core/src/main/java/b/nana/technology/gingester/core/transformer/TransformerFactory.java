package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.annotations.*;
import b.nana.technology.gingester.core.cli.CliParser;
import b.nana.technology.gingester.core.cli.CliSplitter;
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

public final class TransformerFactory {

    private static Materials spiMaterials;

    public static TransformerFactory withSpiProviders() {
        if (spiMaterials == null) spiMaterials = new Materials();
        return new TransformerFactory(spiMaterials);
    }

    public static TransformerFactory withProviders(List<Provider> providers) {
        return new TransformerFactory(new Materials(providers));
    }

    public static TransformerFactory withProvidersByFqdn(List<String> providers) {
        return withProviders(providers.stream()
                .map(fqdn -> {
                    try {
                        return Class.forName(fqdn).getConstructor().newInstance();
                    } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Provider.class::cast)
                .toList());
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformerFactory.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature());


    private final Set<Class<? extends Transformer<?, ?>>> transformers;
    private final Set<Class<? extends Transformer<?, ?>>> pureTransformers;
    private final Map<String, String> caseHints;

    private final Map<Transformer<?, ?>, Object> parameters = new HashMap<>();

    private TransformerFactory(Materials materials) {
        transformers = materials.transformers;
        pureTransformers = materials.pure;
        caseHints = materials.caseHints;
    }

    public <I, O> Transformer<I, O> instance(String name) {
        return instance(name, null);
    }

    public <I, O> Transformer<I, O> instance(String name, JsonNode jsonParameters) {

        List<Class<? extends Transformer<?, ?>>> transformerClasses =
                getTransformersByName(name).toList();

        if (transformerClasses.isEmpty()) {
            List<String> options = getTransformers()
                    .map(this::getUniqueName)
                    .filter(s -> s.endsWith(name))
                    .sorted()
                    .collect(Collectors.toList());
            if (options.isEmpty()) {
                throw new IllegalArgumentException(String.format(
                        "No transformer named %s",
                        name
                ));
            } else if (options.size() == 1) {
                throw new IllegalArgumentException(String.format(
                        "No transformer named %s, maybe %s?",
                        name,
                        options.get(0)
                ));
            } else {
                throw new IllegalArgumentException(String.format(
                        "No transformer named %s, maybe one of %s?",
                        name,
                        String.join(", ", options)
                ));
            }
        } else if (transformerClasses.size() > 1) {
            String uniqueNames = transformerClasses.stream()
                    .map(this::getUniqueName)
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

    public <I, O> Transformer<I, O> instance(Class<? extends Transformer<I, O>> transformerClass, JsonNode jsonParameters) {

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
            Transformer<I, O> instance = constructor.newInstance(parameters);
            this.parameters.put(instance, parameters);
            return instance;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("Calling parameter-rich constructor on " + transformerClass.getCanonicalName() + " failed", e);
        }
    }

    public Optional<Object> getParameters(Transformer<?, ?> transformer) {
        return Optional.ofNullable(parameters.get(transformer));
    }

    public <I, O> Optional<ArrayDeque<Class<? extends Transformer<?, ?>>>> getBridge(Class<I> from, Class<O> to) {

        List<ArrayDeque<Class<? extends Transformer<?, ?>>>> options = pureTransformers.stream()
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
                            return pureTransformers.stream()
                                    .filter(c -> {
                                        Class<?>[] types = TypeResolver.resolveRawArguments(Transformer.class, c);
                                        Class<?> in = types[0];
                                        Class<?> out = types[1];
                                        boolean inIsAssignableFromTail = in.isAssignableFrom(tailType);
                                        boolean outIsANewTypeInBridge = b.stream()
                                                .map(bc -> TypeResolver.resolveRawArguments(Transformer.class, bc)[1])
                                                .noneMatch(out::equals);
                                        return inIsAssignableFromTail && outIsANewTypeInBridge;
                                    })
                                    .map(c -> {
                                        ArrayDeque<Class<? extends Transformer<?, ?>>> next = new ArrayDeque<>(b);
                                        next.add(c);
                                        return next;
                                    });
                        })
                        .toList();
            }
        }

        return Optional.empty();
    }

    public String getUniqueName(Transformer<?, ?> transformer) {
        // noinspection unchecked
        return getUniqueName((Class<? extends Transformer<?, ?>>) transformer.getClass());
    }

    public String getUniqueName(Class<? extends Transformer<?, ?>> transformer) {
        if (transformer.getCanonicalName() == null) return "__anonymous__";
        return getNames(transformer)
                .filter(name -> getTransformersByName(name).skip(1).findFirst().isEmpty())
                .findFirst().orElseThrow(() -> new IllegalStateException("No unique name for " + transformer.getCanonicalName()));
    }

    private Stream<Class<? extends Transformer<?, ?>>> getTransformersByName(String query) {
        return transformers.stream().filter(c -> getNames(c).anyMatch(query::equals));
    }

    private Stream<String> getNames(Class<? extends Transformer<?, ?>> transformer) {
        Names names = transformer.getAnnotation(Names.class);
        int skip = names == null ? 1 : names.value() - 1;
        String[] parts = transformer.getCanonicalName().split("\\.");
        StringBuilder nameBuilder = new StringBuilder();
        return IntStream.range(1, parts.length - 1)
                .mapToObj(i -> parts[parts.length - i])
                .filter(s -> !s.equalsIgnoreCase("transformers"))
                .map(s -> nameBuilder.insert(0, camelCase(s)).toString())
                .skip(skip);
    }

    public Stream<Class<? extends Transformer<?, ?>>> getTransformers() {
        return transformers.stream();
    }

    public <I, O> Stream<String> getTransformerHelps() {
        return transformers.stream()
                .filter(c -> c.getAnnotation(Deprecated.class) == null)
                .map(transformerClass -> {

                    // TODO indenting is now spread out over the Main class and here, would be nice to consolidate

                    String uniqueName = getUniqueName(transformerClass);
                    StringBuilder help = new StringBuilder(uniqueName);

                    // noinspection unchecked
                    getParameterRichConstructor((Class<? extends Transformer<I, O>>) transformerClass).ifPresent(constructor -> {
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

                    String traits = getTraits(transformerClass).toString();
                    if (!traits.isEmpty())
                        help.append("  ++ ").append(traits).append(" ++");

                    Optional.ofNullable(transformerClass.getAnnotation(Description.class))
                            .map(description -> "\n        " + description.value())
                            .ifPresent(help::append);

                    Arrays.stream(transformerClass.getAnnotationsByType(Example.class))
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
                                            .append("  ++ ")
                                            .append(example.description())
                                            .append(" ++");
                                }

                                return stringBuilder.toString();

                            })
                            .forEach(help::append);

                    return help.toString();
                })
                .sorted();
    }

    public <I, O> Optional<? extends Constructor<? extends Transformer<I, O>>> getParameterRichConstructor(Class<? extends Transformer<I, O>> transformerClass) {
        // noinspection unchecked
        return Arrays.stream(transformerClass.getConstructors())
                .map(c -> (Constructor<? extends Transformer<I, O>>) c)
                .filter(method -> method.getParameterCount() == 1 && method.getParameterTypes()[0].getSimpleName().equals("Parameters"))
                .reduce((a, b) -> { throw new IllegalStateException("Found multiple constructors accepting Parameters"); } );
    }

    public Traits getTraits(Class<? extends Transformer<?, ?>> transformerClass) {
        return new Traits(transformerClass);
    }

    private String camelCase(String name) {
        return caseHints.getOrDefault(name, Character.toUpperCase(name.charAt(0)) + name.substring(1));
    }



    public Optional<CheckExampleException> checkExample(Class<? extends Transformer<?, ?>> transformer, Example example) {
        return checkExample(getUniqueName(transformer), example);
    }

    public Optional<CheckExampleException> checkExample(String uniqueName, Example example) {
        if (example.test()) {
            String cli = getExampleCli(uniqueName, example);
            try {
                CliParser.parse(new FlowBuilder(), CliSplitter.split("-t " + uniqueName + " " + example.example()));
            } catch (Exception e) {
                return Optional.of(new CheckExampleException(cli, e));
            }
        }
        return Optional.empty();
    }

    private String getExampleCli(String uniqueName, Example example) {
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
