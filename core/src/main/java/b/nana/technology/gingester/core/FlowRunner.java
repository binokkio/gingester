package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.configuration.ControllerConfiguration;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.Controller;
import b.nana.technology.gingester.core.reporting.Reporter;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import b.nana.technology.gingester.core.transformers.As;
import b.nana.technology.gingester.core.transformers.Is;
import b.nana.technology.graphtxt.GraphTxt;
import b.nana.technology.graphtxt.Node;
import b.nana.technology.graphtxt.SimpleNode;
import net.jodah.typetools.TypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FlowRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowRunner.class);

    private final LinkedHashMap<Id, ControllerConfiguration<?, ?>> configurations = new LinkedHashMap<>();
    private final LinkedHashMap<Id, Controller<?, ?>> controllers = new LinkedHashMap<>();
    private final Phaser phaser = new Phaser(1);
    private final AtomicBoolean stopping = new AtomicBoolean();

    private final FlowBuilder flowBuilder;
    private final IdFactory idFactory;

    private Thread seedThread;

    public FlowRunner(FlowBuilder flowBuilder) {
        this.flowBuilder = flowBuilder;
        this.idFactory = flowBuilder.getIdFactory();
    }

    public String render() {

        configure();
        setupElog();
        setupSeed();

        if (flowBuilder.goal == Goal.VIEW_BRIDGES) {
            explore();
        }

        List<Node> nodes = flowBuilder.nodes.entrySet().stream()
                .map(entry -> SimpleNode.of(
                        entry.getKey().toString(),
                        Stream.concat(entry.getValue().getLinks().values().stream(), entry.getValue().getExcepts().stream())
                                .map(s -> s.substring(1))  // remove leading scope delimiter, TODO
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());

        return new GraphTxt(nodes).getText();
    }

    public void run() {
        if (flowBuilder.goal == Goal.RUN) {
            configure();
            setupElog();
            setupSeed();
            explore();
            align();
            initialize();
            start();
        } else {
            System.out.println(render());
        }
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

        if (!stopping.getAndSet(true))
            seedThread.interrupt();  // TODO improve

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

        flowBuilder.nodes.forEach((id, node) -> {

            @SuppressWarnings("unchecked")
            ControllerConfiguration<I, O> configuration = configure(id, (Transformer<I, O>) node.requireTransformer());

            configuration
                    .links(node.getLinks(), idFactory)
                    .syncs(node.getSyncs(), idFactory)
                    .excepts(node.getExcepts(), idFactory);

            node.getMaxWorkers().ifPresent(configuration::maxWorkers);
            node.getMaxQueueSize().ifPresent(configuration::maxQueueSize);
            node.getMaxBatchSize().ifPresent(configuration::maxBatchSize);
            node.getReport().ifPresent(configuration::report);
            node.getSetupControls().getAcksCounter().ifPresent(configuration::acksCounter);

            configurations.put(id, configuration);
        });

        if (configurations.values().stream().noneMatch(ControllerConfiguration::getReport)) {
            configurations.values().stream()
                    .filter(c -> !c.getId().getLocalId().startsWith("__"))
                    .filter(c -> c.getLinks().isEmpty())
                    .forEach(c -> c.report(true));
        }
    }

    private <I, O> ControllerConfiguration<I, O> configure(Id id, Transformer<I, O> transformer) {
        return new ControllerConfiguration<>(
                id,
                transformer,
                new ControllerConfigurationInterface()
        );
    }

    private void setupElog() {
        for (ControllerConfiguration<?, ?> catcher : configurations.values()) {
            for (Id handlerId : catcher.getExcepts()) {
                ControllerConfiguration<?, ?> handler = configurations.get(handlerId);
                if (handler == null) throw new IllegalStateException(catcher.getId() + " excepts to " + handlerId + " which does not exist");
                if (handlerId != Id.ELOG && handler.getExcepts().isEmpty()) handler.excepts(List.of(Id.ELOG));
            }
        }
    }

    private void setupSeed() {
        List<Id> noIncoming = new ArrayList<>(configurations.keySet());
        noIncoming.remove(Id.SEED);
        configurations.values().stream()
                .flatMap(c -> Stream.concat(c.getLinks().values().stream(), c.getExcepts().stream()))
                .forEach(noIncoming::remove);
        ControllerConfiguration<?, ?> seedConfiguration = configurations.get(Id.SEED);
        List<Id> seedLinks = new ArrayList<>(seedConfiguration.getLinks().values());
        seedLinks.addAll(noIncoming.isEmpty() ? configurations.keySet() : noIncoming);  // link seed with everything if noIncoming is empty for circular route detection
        seedLinks.remove(Id.SEED);
        seedConfiguration.links(seedLinks);
        flowBuilder.getNode(Id.SEED).setLinks(seedLinks.stream().map(Id::getGlobalId).collect(Collectors.toList()));
    }

    private void explore() {
        Set<Id> seen = new HashSet<>();
        explore(Id.SEED.getLocalId(), Id.SEED, new ArrayDeque<>(), false, seen);
        configurations.keySet().stream().filter(id -> !seen.contains(id)).forEach(id -> explore(id.toString(), id, new ArrayDeque<>(), false, new HashSet<>()));
    }

    private void explore(String nextName, Id nextId, ArrayDeque<Id> route, boolean maybeBridge, Set<Id> seen) {
        seen.add(nextId);
        if (route.contains(nextId)) {
            Iterator<Id> iterator = route.iterator();
            while (!iterator.next().equals(nextId)) iterator.remove();
            throw new IllegalStateException("Circular route detected: " + route.stream().map(Id::toString).collect(Collectors.joining(" -> ")) + " -> " + nextId);
        } else {
            if (!configurations.containsKey(nextId)) throw new IllegalStateException(route.getLast() + " links to " + nextId + " which does not exist");
            if (maybeBridge) seen.addAll(maybeBridge(route.getLast(), nextName, nextId));
            route.add(nextId);
            configurations.get(nextId).getLinks().forEach((name, id) -> explore(name, id, new ArrayDeque<>(route), true, seen));
            Iterator<Id> iterator = route.descendingIterator();
            while (iterator.hasNext()) {
                Id id = iterator.next();
                if (id == Id.ELOG) break;  // elog excepts to itself but is assumed to never throw
                ControllerConfiguration<?, ?> controller = configurations.get(id);
                if (!controller.getExcepts().isEmpty()) {
                    controller.getExcepts().forEach(eh -> explore(eh.toString(), eh, new ArrayDeque<>(route), false, seen));
                    break;
                }
            }
        }
    }

    private <I, O> Set<Id> maybeBridge(Id upstreamId, String linkName, Id downstreamId) {

        ControllerConfiguration<?, ?> upstream = configurations.get(upstreamId);
        ControllerConfiguration<?, ?> downstream = configurations.get(downstreamId);

        Class<?> output = upstream.getOutputType();
        Class<?> input = downstream.getInputType();

        if (input.equals(TypeResolver.Unknown.class) || input.isAssignableFrom(output)) return Set.of();

        if (output.equals(TypeResolver.Unknown.class) || output.equals(Object.class)) {

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Bridging from " + upstreamId + " to " + downstreamId + " with a dynamic bridge, consider using -i/--is if you know the correct type");

            @SuppressWarnings("unchecked")
            Transformer<I, O> as = (Transformer<I, O>) new As(input);
            Id id = flowBuilder.splice(as, upstream.getId().getGlobalId(), linkName).getLastId();

            configurations.put(id, configure(id, as).links(Map.of(linkName, downstreamId)));
            upstream.updateLink(linkName, id);

            return Set.of(id);

        } else {

            Collection<Class<? extends Transformer<?, ?>>> bridge = TransformerFactory.getBridge(output, input)
                    .orElseThrow(() -> new IllegalStateException("Transformations from " + upstreamId + " to " + downstreamId + " must be specified"));  // TODO

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Bridging from " + upstreamId + " to " + downstreamId + " with " + bridge.stream().map(TransformerFactory::getUniqueName).collect(Collectors.joining(" -> ")));

            Set<Id> bridgeIds = new HashSet<>();
            ControllerConfiguration<?, ?> pointer = upstream;
            for (Class<? extends Transformer<?, ?>> transformerClass : bridge) {

                @SuppressWarnings("unchecked")
                Transformer<I, O> transformer = TransformerFactory.instance((Class<? extends Transformer<I, O>>) transformerClass, null);
                Id id = flowBuilder.splice(transformer, pointer.getId().getGlobalId(), linkName).getLastId();

                ControllerConfiguration<I, O> configuration = configure(id, transformer).links(Map.of(linkName, downstreamId));
                configurations.put(id, configuration);
                pointer.updateLink(linkName, id);
                pointer = configuration;

                bridgeIds.add(id);
            }

            // dynamic bridging provided by As not necessary, swap for Is
            if (downstream.getTransformer() instanceof As)
                downstream.updateTransformer(new Is(output));

            return bridgeIds;
        }
    }

    private void align() {
        flowBuilder.nodes.forEach((id, node) -> {
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
                            .map(Id::toString)
                            .collect(Collectors.joining(", "));
                    if (!incompatible.isEmpty()) {
                        throw new IllegalStateException(id + " requires outgoing links to be sync, but " + incompatible + " are async (maxWorkers > 0)");
                    }
                } else if (setupControls.getRequireOutgoingAsync()) {
                    String incompatible = outgoingConfigurations
                            .peek(o -> { if (o.getMaxWorkers().isEmpty()) o.maxWorkers(1); })
                            .filter(o -> o.getMaxWorkers().get() == 0)
                            .map(ControllerConfiguration::getId)
                            .map(Id::toString)
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

        seedThread = Thread.currentThread();

        controllers.values().forEach(Controller::open);
        phaser.arriveAndAwaitAdvance();

        if (flowBuilder.shutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
        }

        Reporter reporter = null;
        if (flowBuilder.reportIntervalSeconds > 0) {
            reporter = new Reporter(flowBuilder.reportIntervalSeconds, controllers.values());
            reporter.start();
        }

        @SuppressWarnings("unchecked")
        Controller<Object, Object> seedController = (Controller<Object, Object>) controllers.get(Id.SEED);
        Context seedContext = Context.newSeedContext(seedController, flowBuilder.parentContext, flowBuilder.seedStash);
        seedController.transform(seedContext, flowBuilder.seedValue);

        phaser.arriveAndAwaitAdvance();
        controllers.values().forEach(Controller::close);
        phaser.arriveAndAwaitAdvance();

        if (reporter != null) reporter.stop();

        stopping.set(true);  // set stopping true, flow stopped naturally
    }

    public class ControllerConfigurationInterface {
        public LinkedHashMap<Id, ControllerConfiguration<?, ?>> getControllers() {
            return configurations;
        }
    }

    public class ControllerInterface {

        private final Id controllerId;

        private ControllerInterface(Id controllerId) {
            this.controllerId = controllerId;
        }

        public Phaser getPhaser() {
            return phaser;
        }

        public Collection<ControllerConfiguration<?, ?>> getConfigurations() {
            return configurations.values();
        }

        public Collection<Controller<?, ?>> getControllers() {
            return controllers.values();
        }

        public Controller<?, ?> getController(Id id) {
            Controller<?, ?> controller = controllers.get(id);
            if (controller == null) throw new IllegalArgumentException("No controller has id " + id);
            return controller;
        }

        public boolean isDebugModeEnabled() {
            return flowBuilder.debugMode;
        }

        public boolean isExceptionHandler() {
            return configurations.values().stream().anyMatch(c -> c.getExcepts().contains(controllerId));
        }
    }

    public enum Goal {
        VIEW,
        VIEW_BRIDGES,
        RUN
    }
}
