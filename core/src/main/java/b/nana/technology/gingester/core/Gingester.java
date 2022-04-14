package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.cli.CliParser;
import b.nana.technology.gingester.core.configuration.ControllerConfiguration;
import b.nana.technology.gingester.core.configuration.GingesterConfiguration;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.configuration.TransformerConfiguration;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.Controller;
import b.nana.technology.gingester.core.controller.Worker;
import b.nana.technology.gingester.core.reporting.Reporter;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import b.nana.technology.gingester.core.transformers.Passthrough;
import net.jodah.typetools.TypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.concurrent.Phaser;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static b.nana.technology.gingester.core.configuration.TransformerConfigurationSetupControlsCombiner.combine;

public final class Gingester {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gingester.class);

    private final LinkedHashMap<String, TransformerConfiguration> transformerConfigurations = new LinkedHashMap<>();
    private final LinkedHashMap<String, SetupControls> setupControls = new LinkedHashMap<>();
    private final LinkedHashMap<String, ControllerConfiguration<?, ?>> configurations = new LinkedHashMap<>();
    private final LinkedHashMap<String, Controller<?, ?>> controllers = new LinkedHashMap<>();
    private final Map<String, Phaser> phasers = new HashMap<>();
    private final Phaser phaser = new Phaser();

    private final Set<String> excepts;
    private final int reportingIntervalSeconds;
    private final boolean gracefulShutdown;
    private final String defaultAttachTarget;

    /**
     * Construct Gingester with cli instructions.
     * <p>
     * The given cli string will be rendered using the Apache Freemarker template engine using the square-bracket-tag
     * and square-bracket-interpolation syntax.
     *
     * @param cli cli instructions template
     */
    public Gingester(String cli) {
        this(cli, Collections.emptyMap());
    }

    /**
     * Construct Gingester with cli instructions.
     * <p>
     * The given cli string will be rendered using the Apache Freemarker template engine using the square-bracket-tag
     * and square-bracket-interpolation syntax.
     *
     * @param cli cli instructions template
     * @param parameters the parameters for the template, e.g. a Java Map
     */
    public Gingester(String cli, Object parameters) {
        this(CliParser.parse(cli, parameters));
    }

    /**
     * Construct Gingester with cli instructions.
     * <p>
     * The string obtained from the given URL will be rendered using the Apache Freemarker template engine using the
     * square-bracket-tag and square-bracket-interpolation syntax.
     *
     * @param cli URL for the cli instructions
     */
    public Gingester(URL cli) {
        this(cli, Collections.emptyMap());
    }

    /**
     * Construct Gingester with cli instructions.
     * <p>
     * The string obtained from the given URL will be rendered using the Apache Freemarker template engine using the
     * square-bracket-tag and square-bracket-interpolation syntax.
     *
     * @param cli URL for the cli instructions
     * @param parameters the parameters for the template, e.g. a Java Map
     */
    public Gingester(URL cli, Object parameters) {
        this(CliParser.parse(cli, parameters));
    }

    /**
     * Construct Gingester with the given configuration.
     *
     * @param configuration the configuration to execute
     */
    public Gingester(GingesterConfiguration configuration) {

        excepts = configuration.excepts.isEmpty() ? Collections.singleton("__elog__") : new HashSet<>(configuration.excepts);
        reportingIntervalSeconds = configuration.report == null ? 0 : configuration.report;
        gracefulShutdown = configuration.gracefulShutdown == null || configuration.gracefulShutdown;

        String lastId = null;
        for (TransformerConfiguration transformer : configuration.transformers) {
            lastId = add(transformer);
        }
        defaultAttachTarget = lastId;

        TransformerConfiguration last = transformerConfigurations.get(lastId);
        if (last.getLinks().isPresent()) {
            List<String> newLinks = new ArrayList<>(last.getLinks().get());
            newLinks.remove("__maybe_next__");
            last.links(newLinks);
        }
    }

    /**
     * Attach consumer to the "last" transformer.
     *
     * @param <T> the consumer type
     * @param consumer the consumer
     * @return this gingester
     */
    public <T> Gingester attach(Consumer<T> consumer) {
        attach(consumer, defaultAttachTarget);
        return this;
    }

    /**
     * Attach consumer to transformer.
     *
     * @param <T> the consumer type
     * @param consumer the consumer
     * @param targetId the id of the transformer whose output will be consumed
     * @return this gingester
     */
    public <T> Gingester attach(Consumer<T> consumer, String targetId) {
        Transformer<T, T> transformer = (context, in, out) -> consumer.accept(in);
        String id = add(new TransformerConfiguration().transformer("Consumer", transformer).links(Collections.emptyList()));
        attach(id, targetId);
        return this;
    }

    /**
     * Attach bi-consumer to the "last" transformer.
     *
     * @param <T> the bi-consumer type
     * @param biConsumer the bi-consumer
     * @return this gingester
     */
    public <T> Gingester attach(BiConsumer<Context, T> biConsumer) {
        attach(biConsumer, defaultAttachTarget);
        return this;
    }

    /**
     * Attach bi-consumer to transformer.
     *
     * @param <T> the bi-consumer type
     * @param biConsumer the bi-consumer
     * @param targetId the id of the transformer whose output will be consumed
     * @return this gingester
     */
    public <T> Gingester attach(BiConsumer<Context, T> biConsumer, String targetId) {
        Transformer<T, T> transformer = (context, in, out) -> biConsumer.accept(context, in);
        String id = add(new TransformerConfiguration().transformer("Consumer", transformer).links(Collections.emptyList()));
        attach(id, targetId);
        return this;
    }

    private String add(TransformerConfiguration configuration) {
        String id = getId(configuration);
        transformerConfigurations.put(id, configuration);
        return id;
    }

    private void attach(String id, String targetId) {
        TransformerConfiguration target = transformerConfigurations.get(targetId);
        List<String> links = new ArrayList<>(target.getLinks().orElse(Collections.emptyList()));
        links.add(id);
        target.links(links);
    }

    /**
     * Run the configured transformations.
     * <p>
     * This will run through the following steps:
     * <ul>
     * <li>call {@link Transformer#setup(SetupControls)} on all transformers
     * <li>consolidate the transformer setup controls with the transformer configurations
     * <li>create a seed transformer with id __seed__ and link it to all transformers with no incoming links
     * <li>start the workers for each transformer
     * <li>call {@link Transformer#open()} on all transformers from their worker threads and wait for all to open
     * <li>give the seed controller a single input, which it will pass through to its links
     * <li>start a reporting thread if configured
     * <li>block until all transformers are done
     * <li>call {@link Transformer#close()} on all transformers from one of their worker threads and wait for all to close
     * </ul>
     */
    public void run() {
        configure();
        setupSeed();
        setupExceptionLogger();
        explore("__seed__", "__seed__", new ArrayDeque<>());
        align();
        initialize();
        start();
    }

    private void configure() {

        if (transformerConfigurations.isEmpty()) {
            throw new IllegalStateException("No transformers configured");
        }

        transformerConfigurations.forEach((id, transformerConfiguration) -> {

            Transformer<?, ?> transformer = transformerConfiguration.getTransformer()
                    .orElseThrow(() -> new IllegalStateException("TransformerConfiguration does not contain transformer"));

            SetupControls setupControls = new SetupControls(transformer, phasers);
            transformer.setup(setupControls);

            ControllerConfiguration<?, ?> configuration = combine(new ControllerConfigurationInterface(), id, transformer, transformerConfiguration, setupControls);
            resolveMaybeNext(id).ifPresentOrElse(
                    configuration::replaceMaybeNextLink,
                    configuration::removeMaybeNextLink
            );

            this.setupControls.put(id, setupControls);
            this.configurations.put(id, configuration);
        });

        if (transformerConfigurations.values().stream().noneMatch(c -> c.getReport().filter(r -> r).isPresent())) {
            configurations.values().stream()
                    .filter(c -> c.getLinks().isEmpty())
                    .forEach(c -> c.report(true));
        }
    }

    private void setupSeed() {
        Set<String> noIncoming = new HashSet<>(configurations.keySet());
        noIncoming.removeAll(excepts);
        configurations.values().stream()
                .flatMap(c -> Stream.concat(c.getLinks().values().stream(), c.getExcepts().stream()))
                .forEach(noIncoming::remove);
        configurations.put("__seed__", new ControllerConfiguration<>(new ControllerConfigurationInterface())
                .id("__seed__")
                .transformer(new Passthrough())
                .links(new ArrayList<>(noIncoming.isEmpty() ? configurations.keySet() : noIncoming))  // if noIncoming is empty, link seed to everything for circular route detection
                .excepts(new ArrayList<>(excepts)));
    }

    private void setupExceptionLogger() {
        configurations.put("__elog__", new ControllerConfiguration<>(new ControllerConfigurationInterface())
                .id("__elog__")
                .transformer((context, exception, out) -> {
                    if (LOGGER.isWarnEnabled()) {
                        String description = context.fetchReverse("description").map(Object::toString).collect(Collectors.joining(" :: "));
                        if (description.isEmpty()) {
                            LOGGER.warn(
                                    String.format(
                                            "%s during %s::%s",
                                            ((Exception) exception).getClass().getSimpleName(),
                                            context.fetch("transformer").findFirst().orElseThrow(),
                                            context.fetch("method").findFirst().orElseThrow())
                                    , (Throwable) exception
                            );
                        } else {
                            LOGGER.warn(
                                    String.format(
                                            "%s during %s::%s of '%s'",
                                            ((Exception) exception).getClass().getSimpleName(),
                                            context.fetch("transformer").findFirst().orElseThrow(),
                                            context.fetch("method").findFirst().orElseThrow(),
                                            description)
                                    , (Throwable) exception
                            );
                        }
                    }
                }));

        configurations.values()
                .stream().flatMap(c -> c.getExcepts().stream())
                .map(configurations::get)
                .filter(c -> !c.getId().equals("__elog__"))
                .filter(c -> c.getExcepts().isEmpty())
                .forEach(c -> c.excepts(Collections.singletonList("__elog__")));
    }

    private void explore(String nextName, String nextId, ArrayDeque<String> route) {
        if (route.contains(nextId)) {
            Iterator<String> iterator = route.iterator();
            while (!iterator.next().equals(nextId)) iterator.remove();
            throw new IllegalStateException("Circular route detected: " + String.join(" -> ", route) + " -> " + nextId);
        } else {
            if (!route.isEmpty()) maybeBridge(route.getLast(), nextName, nextId);
            route.add(nextId);
            configurations.get(nextId).getLinks().forEach((name, id) -> explore(name, id, new ArrayDeque<>(route)));
            Iterator<String> iterator = route.descendingIterator();
            while (iterator.hasNext()) {
                String id = iterator.next();
                boolean isExceptionHandler = excepts.contains(id) ||
                        configurations.values().stream().anyMatch(c -> c.getExcepts().contains(id));
                if (isExceptionHandler) break;  // TODO this only works as long as a controller is not used as both a normal link and an exception handler
                ControllerConfiguration<?, ?> controller = configurations.get(id);
                if (!controller.getExcepts().isEmpty()) {
                    for (String exceptionHandler : controller.getExcepts()) {
                        explore(exceptionHandler, exceptionHandler, new ArrayDeque<>(route));
                    }
                }
            }
        }
    }

    private <I, O> void maybeBridge(String upstreamId, String linkName, String downstreamId) {

        ControllerConfiguration<?, ?> upstream = configurations.get(upstreamId);
        ControllerConfiguration<?, ?> downstream = configurations.get(downstreamId);

        Class<?> output = upstream.getOutputType();
        Class<?> input = downstream.getInputType();

        if (Stream.of(output, input).noneMatch(c -> c.equals(TypeResolver.Unknown.class) || c.equals(Object.class))) {
            if (!input.isAssignableFrom(output)) {

                Collection<Class<? extends Transformer<?, ?>>> bridge = TransformerFactory.getBridge(output, input)
                        .orElseThrow(() -> new IllegalStateException("Transformations from " + upstreamId + " to " + downstreamId + " must be specified"));  // TODO

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Bridging from " + upstreamId + " to " + downstreamId + " with " + bridge.stream().map(TransformerFactory::getUniqueName).collect(Collectors.joining(" -> ")));
                }

                ControllerConfiguration<?, ?> pointer = upstream;
                for (Class<? extends Transformer<?, ?>> transformerClass : bridge) {

                    Transformer<I, O> transformer = TransformerFactory.instance((Class<? extends Transformer<I, O>>) transformerClass, null);
                    String id = add(new TransformerConfiguration().transformer(transformer));

                    SetupControls setupControls = new SetupControls(transformer, phasers);
                    transformer.setup(setupControls);
                    this.setupControls.put(id, setupControls);

                    ControllerConfiguration<I, O> configuration = new ControllerConfiguration<I, O>(new ControllerConfigurationInterface())
                            .id(id)
                            .transformer(transformer)
                            .links(Collections.singletonMap(linkName, downstreamId));

                    configurations.put(id, configuration);
                    pointer.updateLink(linkName, id);
                    pointer = configuration;
                }
            }
        }
    }

    private void align() {
        setupControls.forEach((id, setupControls) -> {
            if (setupControls.getRequireOutgoingSync() || setupControls.getRequireOutgoingAsync()) {
                if (setupControls.getRequireOutgoingSync() && setupControls.getRequireOutgoingAsync()) {
                    throw new IllegalStateException("SetupControls for " + id + " require outgoing to be both sync and async");
                }

                Stream<ControllerConfiguration<?, ?>> outgoingConfigurations = configurations.get(id).getLinks().values().stream().map(configurations::get);

                if (setupControls.getRequireOutgoingSync()) {
                    String incompatible = outgoingConfigurations
                            .filter(o -> o.getMaxWorkers().isPresent())
                            .filter(o -> o.getMaxWorkers().get() > 0)
                            .map(ControllerConfiguration::getId)
                            .collect(Collectors.joining(", "));
                    if (!incompatible.isEmpty()) {
                        throw new IllegalStateException(id + " requires outgoing links to be sync, but " + incompatible + " are async (maxWorkers > 0)");
                    }
                } else if (setupControls.getRequireOutgoingAsync()) {
                    String incompatible = outgoingConfigurations
                            .peek(o -> { if (o.getMaxWorkers().isEmpty()) o.maxWorkers(1); })
                            .filter(o -> o.getMaxWorkers().get() == 0)
                            .map(ControllerConfiguration::getId)
                            .collect(Collectors.joining(", "));
                    if (!incompatible.isEmpty()) {
                        throw new IllegalStateException(id + " requires outgoing links to be async, but " + incompatible + " are sync (maxWorkers == 0)");
                    }
                }
            }
        });
    }

    private void initialize() {

        configurations.forEach((id, configuration) ->
                controllers.put(id, new Controller<>(configuration, new ControllerInterface(id))));

        controllers.values().forEach(Controller::initialize);
        controllers.values().forEach(Controller::discoverIncoming);
        controllers.values().forEach(Controller::discoverDownstream);
        controllers.values().forEach(Controller::discoverSyncs);
    }

    private void start() {

        if (gracefulShutdown) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
        }

        controllers.values().forEach(Controller::open);
        phaser.awaitAdvance(0);

        Reporter reporter = new Reporter(reportingIntervalSeconds, controllers.values());
        if (reportingIntervalSeconds > 0) reporter.start();

        Controller<Object, Object> seedController = (Controller<Object, Object>) controllers.get("__seed__");
        Context seed = Context.newSeedContext(seedController);
        seedController.accept(new Batch<>(seed, "seed signal"));
        seedController.finish(null, seed);

        try {

            for (Controller<?, ?> controller : controllers.values()) {
                for (Worker worker : controller.workers) {
                    worker.join();
                }
            }

            if (reportingIntervalSeconds > 0) {
                reporter.stop();
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);  // TODO
        }
    }

    private void onShutdown() {

        LOGGER.warn("Received shutdown signal, gracefully shutting down");

        Controller<?, ?> seedController = controllers.get("__seed__");
        for (Worker seedWorker : seedController.workers) {
            seedWorker.interrupt();
        }

        for (Controller<?, ?> controller : controllers.values()) {
            for (Worker worker : controller.workers) {
                try {
                    LOGGER.info("Waiting for " + worker.getName());
                    worker.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getId(TransformerConfiguration configuration) {
        if (configuration.getId().isPresent()) {
            String id = configuration.getId().get();
            if (transformerConfigurations.containsKey(id)) {
                throw new IllegalArgumentException("Transformer id " + id + " already in use");
            }
            return id;
        } else {
            String name = configuration.getName().orElseThrow();
            String id = name;
            int i = 1;
            while (transformerConfigurations.containsKey(id)) {
                id = name + '_' + i++;
            }
            return id;
        }
    }

    private Optional<String> resolveMaybeNext(String from) {
        boolean next = false;
        for (String id : transformerConfigurations.keySet()) {
            if (next) {
                return Optional.of(id);
            } else if (id.equals(from)) {
                next = true;
            }
        }
        return Optional.empty();
    }

    public class ControllerConfigurationInterface {
        public LinkedHashMap<String, ControllerConfiguration<?, ?>> getControllers() {
            return configurations;
        }
    }

    public class ControllerInterface {

        private final String controllerId;

        private ControllerInterface(String controllerId) {
            this.controllerId = controllerId;
        }

        public Phaser getPhaser() {
            return phaser;
        }

        public Collection<Controller<?, ?>> getControllers() {
            return controllers.values();
        }

        public Optional<Controller<?, ?>> getController(String id) {
            Controller<?, ?> controller = controllers.get(id);
            if (controller == null) throw new IllegalArgumentException("No controller has id " + id);
            return Optional.of(controller);
        }

        public boolean isExceptionHandler() {
            return excepts.contains(controllerId) ||
                    configurations.values().stream().anyMatch(c -> c.getExcepts().contains(controllerId));
        }
    }
}
