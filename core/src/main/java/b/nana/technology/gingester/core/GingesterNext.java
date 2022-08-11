package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.configuration.ControllerConfiguration;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.Controller;
import b.nana.technology.gingester.core.controller.Worker;
import b.nana.technology.gingester.core.reporting.Reporter;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import net.jodah.typetools.TypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GingesterNext {

    private static final Logger LOGGER = LoggerFactory.getLogger(GingesterNext.class);

    private final Gingester flowBuilder;
    private final LinkedHashMap<String, ControllerConfiguration<?, ?>> configurations = new LinkedHashMap<>();
    private final LinkedHashMap<String, Controller<?, ?>> controllers = new LinkedHashMap<>();
    private final Phaser phaser = new Phaser();
    private final AtomicBoolean stopping = new AtomicBoolean();

    private int reportIntervalSeconds = 1;
    private boolean debugMode = true;
    private boolean shutdownHook;

    public GingesterNext(Gingester flowBuilder) {
        this.flowBuilder = flowBuilder;  // TODO defensive copy?
    }

    /**
     * Run the configured transformations.
     * <p>
     * This will run through the following steps:
     * <ul>
     * <li>link the seed transformer to all transformers with no incoming links
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
        setupElog();
        setupSeed();
        explore("__seed__", "__seed__", new ArrayDeque<>(), false);
        align();
        initialize();
        start();
    }

    /**
     * Attempt to gracefully stop the running flow.
     * <p>
     * This will interrupt the seed worker and all workers belonging to transformations that are directly
     * linked by the seed transformer (i.e. those with no configured incoming links).
     * <p>
     * For graceful stopping to work properly transformers that must not be interrupted must be running
     * asynchronously from the aforementioned workers.
     */
    public void stop() {

        phaser.awaitAdvance(0);

        if (!stopping.getAndSet(true)) {
            Controller<?, ?> seedController = controllers.get("__seed__");
            Stream.of(List.of(seedController), seedController.links.values())
                    .flatMap(Collection::stream)
                    .map(c -> c.workers)
                    .flatMap(Collection::stream)
                    .forEach(Worker::interrupt);  // TODO instead of always interrupting allow transformers to override a callback and only interrupt if it returns false
        }

        phaser.awaitAdvance(1);
        phaser.awaitAdvance(2);
    }

    private void shutdownHook() {

        if (!stopping.get()) {
            LOGGER.warn("Shutdown requested, attempting graceful shutdown");
        }

        stop();
    }

    private <I, O> void configure() {

        flowBuilder.getNodes().forEach((id, node) -> {

            ControllerConfiguration<I, O> configuration = new ControllerConfiguration<>(new ControllerConfigurationInterface());

            configuration
                    .id(id)
                    .transformer((Transformer<I, O>) node.requireTransformer())
//                    .report(node.getReport().orElse(false))
                    .links(node.getLinks())
                    .syncs(node.getSyncs())
                    .excepts(node.getExcepts());

//            choose(setup::getMaxBatchSize, transformer::getMaxBatchSize).ifPresent(configuration::maxBatchSize);
//            choose(setup::getMaxQueueSize, transformer::getMaxQueueSize).ifPresent(configuration::maxQueueSize);
//            choose(setup::getMaxWorkers, transformer::getMaxWorkers).ifPresent(configuration::maxWorkers);

//            setup.getAcksCounter().ifPresent(configuration::acksCounter);

            this.configurations.put(id, configuration);
        });

        if (configurations.values().stream().noneMatch(ControllerConfiguration::getReport)) {
            configurations.values().stream()
                    .filter(c -> !c.getId().startsWith("__"))
                    .filter(c -> c.getLinks().isEmpty())
                    .forEach(c -> c.report(true));
        }
    }

    private void setupElog() {
        for (ControllerConfiguration<?, ?> catcher : configurations.values()) {
            for (String handlerId : catcher.getExcepts()) {
                ControllerConfiguration<?, ?> handler = configurations.get(handlerId);
                if (handler == null) throw new IllegalStateException(catcher.getId() + " excepts to " + handlerId + " which does not exist");
                if (!handlerId.equals("__elog__") && handler.getExcepts().isEmpty()) handler.excepts(Collections.singletonList("__elog__"));
            }
        }
    }

    private void setupSeed() {
        List<String> noIncoming = new ArrayList<>(configurations.keySet());
        noIncoming.remove("__seed__");
        configurations.values().stream()
                .flatMap(c -> Stream.concat(c.getLinks().values().stream(), c.getExcepts().stream()))
                .forEach(noIncoming::remove);
        ControllerConfiguration<?, ?> seedConfiguration = configurations.get("__seed__");
        List<String> seedLinks = new ArrayList<>(seedConfiguration.getLinks().values());
        seedLinks.addAll(noIncoming.isEmpty() ? configurations.keySet() : noIncoming);  // link seed with everything if noIncoming is empty for circular route detection
        seedLinks.remove("__seed__");
        seedConfiguration.links(seedLinks);
    }

    private void explore(String nextName, String nextId, ArrayDeque<String> route, boolean maybeBridge) {
        if (route.contains(nextId)) {
            Iterator<String> iterator = route.iterator();
            while (!iterator.next().equals(nextId)) iterator.remove();
            throw new IllegalStateException("Circular route detected: " + String.join(" -> ", route) + " -> " + nextId);
        } else {
            if (!configurations.containsKey(nextId)) throw new IllegalStateException(route.getLast() + " links to " + nextId + " which does not exist");
            if (maybeBridge) maybeBridge(route.getLast(), nextName, nextId);
            route.add(nextId);
            configurations.get(nextId).getLinks().forEach((name, id) -> explore(name, id, new ArrayDeque<>(route), true));
            Iterator<String> iterator = route.descendingIterator();
            while (iterator.hasNext()) {
                String id = iterator.next();
                boolean isExceptionHandler = configurations.values().stream().anyMatch(c -> c.getExcepts().contains(id));
                if (isExceptionHandler) break;  // TODO this only works as long as a controller is not used as both a normal link and an exception handler
                ControllerConfiguration<?, ?> controller = configurations.get(id);
                if (!controller.getExcepts().isEmpty()) {
                    for (String exceptionHandler : controller.getExcepts()) {
                        explore(exceptionHandler, exceptionHandler, new ArrayDeque<>(route), false);
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

                    String id = flowBuilder
                            .splice(transformer, pointer.getId(), linkName)
                            .getLastId();

                    // TODO converge with similar code in configure()
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
        flowBuilder.getNodes().forEach((id, node) -> {
            SetupControls setupControls = node.getSetupControls();
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

        controllers.values().forEach(Controller::open);
        phaser.awaitAdvance(0);

        if (shutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
        }

        Reporter reporter = null;
        if (reportIntervalSeconds > 0) {
            reporter = new Reporter(reportIntervalSeconds, controllers.values());
            reporter.start();
        }

        Controller<Object, Object> seedController = (Controller<Object, Object>) controllers.get("__seed__");
        Context seed = Context.newSeedContext(seedController);
        seedController.accept(new Batch<>(seed, "seed signal"));
        seedController.finish(null, seed);

        phaser.awaitAdvance(1);
        phaser.awaitAdvance(2);

        if (reporter != null) reporter.stop();

        stopping.set(true);  // set stopping true, flow stopped naturally
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

        public boolean isDebugModeEnabled() {
            return debugMode;
        }

        public boolean isExceptionHandler() {
            return configurations.values().stream().anyMatch(c -> c.getExcepts().contains(controllerId));
        }

        public boolean isStopping() {
            return stopping.get();
        }
    }
}
