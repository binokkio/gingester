package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.configuration.ControllerConfiguration;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.configuration.TransformerConfiguration;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.Controller;
import b.nana.technology.gingester.core.controller.Worker;
import b.nana.technology.gingester.core.reporting.Reporter;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import b.nana.technology.gingester.core.transformers.Seed;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static b.nana.technology.gingester.core.configuration.TransformerConfigurationSetupControlsCombiner.combine;

public final class Gingester {

    private final Object lock = new Object();
    private final LinkedHashMap<String, TransformerConfiguration> transformerConfigurations = new LinkedHashMap<>();
    private final LinkedHashMap<String, SetupControls> setupControls = new LinkedHashMap<>();
    private final LinkedHashMap<String, ControllerConfiguration<?, ?>> configurations = new LinkedHashMap<>();
    private final LinkedHashMap<String, Controller<?, ?>> controllers = new LinkedHashMap<>();
    private final Set<String> open = new HashSet<>();
    private boolean report;

    public void report(Boolean report) {
        this.report = report == null || report;
    }

    public void add(String transformer) {
        configure(c -> c.transformer(transformer));
    }

    public void add(Transformer<?, ?> transformer) {
        configure(c -> c.transformer(transformer));
    }

    public <T> void add(Consumer<T> consumer) {
        configure(c -> c.transformer(consumer));
    }

    public void add(String id, Transformer<?, ?> transformer) {
        configure(c -> {
            c.id(id);
            c.transformer(transformer);
        });
    }

    public <T> void add(String id, Consumer<T> consumer) {
        configure(c -> {
            c.id(id);
            c.transformer(consumer);
        });
    }

    public void configure(Configurator configurator) {
        TransformerConfiguration configuration = new TransformerConfiguration();
        configurator.configure(configuration);
        add(configuration);
    }

    public void add(TransformerConfiguration configuration) {
        String id = getId(configuration);
        transformerConfigurations.put(id, configuration);
    }

    private String getId(TransformerConfiguration configuration) {
        return configuration.getId()
                .filter(id -> {
                    if (transformerConfigurations.containsKey(id)) {
                        throw new IllegalArgumentException("Transformer id " + id + " already in use");
                    }
                    return true;
                })
                .orElseGet(() -> {
                    String name = configuration.getName().orElseThrow();
                    String id = name;
                    int i = 1;
                    while (transformerConfigurations.containsKey(id)) {
                        id = name + '_' + i++;
                    }
                    return id;
                });
    }

    public void run() {
        setup();
        seed();
        start();
    }

    private void setup() {

        if (transformerConfigurations.isEmpty()) {
            throw new IllegalStateException("No transformers configured");
        }

        transformerConfigurations.forEach((id, transformerConfiguration) -> {

            Transformer<?, ?> transformer = transformerConfiguration.getInstance()
                    .orElseGet(() -> {
                        String name = transformerConfiguration.getName()
                                .orElseThrow(() -> new IllegalStateException("Neither a transformer name nor instance was given"));
                        return transformerConfiguration.getParameters()
                                .map(parameters -> TransformerFactory.instance(name, parameters))
                                .orElseGet(() -> TransformerFactory.instance(name));
                    });

            SetupControls setupControls = new SetupControls();
            transformer.setup(setupControls);

            ControllerConfiguration<?, ?> configuration = combine(id, transformer, transformerConfiguration, setupControls);

            this.setupControls.put(id, setupControls);
            this.configurations.put(id, configuration);
        });

        setupControls.forEach((id, setupControls) -> {
            if (setupControls.getRequireOutgoingSync() || setupControls.getRequireOutgoingAsync()) {
                if (setupControls.getRequireOutgoingSync() && setupControls.getRequireOutgoingAsync()) {
                    throw new IllegalStateException();  // TODO
                }

                Stream<ControllerConfiguration<?, ?>> outgoingConfigurations = configurations.get(id).getLinks().stream()
                        .map(link -> link.equals("__maybe_next__") ? resolveMaybeNext(id).orElse("__none__") : link)
                        .map(configurations::get);

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

        if (transformerConfigurations.values().stream().noneMatch(c -> c.getReport().filter(r -> r).isPresent())) {
            configurations.values().forEach(c -> {
                if (c.getLinks().stream()
                        .map(link -> link.equals("__maybe_next__") ? resolveMaybeNext(c.getId()) : Optional.of(link))
                        .noneMatch(Optional::isPresent)) {
                    c.report(true);
                }
            });
        }

        configurations.forEach((id, configuration) ->
                controllers.put(id, new Controller<>(configuration, new ControllerInterface(id))));
    }

    private void seed() {

        Set<String> noIncoming = new HashSet<>(controllers.keySet());
        controllers.values().stream()
                .flatMap(controller -> Stream.of(
                        controller.getLinks().stream().map(link -> link.equals("__maybe_next__") ? resolveMaybeNext(controller.id).orElse("__none__") : link),
                        controller.getExcepts().stream().map(except -> except.equals("__maybe_next__") ? resolveMaybeNext(controller.id).orElse("__none__") : except)  ))
                .reduce(Stream::concat)
                .orElseThrow()
                .forEach(noIncoming::remove);

        controllers.put("__seed__", new Controller<>(
                new ControllerConfiguration<Void, Object>()
                        .id("__seed__")
                        .transformer(new Seed())
                        .links(new ArrayList<>(noIncoming)),
                new ControllerInterface("__seed__")
        ));

        controllers.values().forEach(Controller::initialize);
        controllers.values().forEach(Controller::discover);
    }

    private void start() {

        controllers.values().forEach(Controller::open);

        try {
            synchronized (lock) {
                while (controllers.size() > open.size()) {
                    lock.wait();
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);  // TODO
        }

        controllers.values().forEach(Controller::start);

        Reporter reporter = new Reporter(controllers.values());
        if (report) reporter.start();

        Controller<Void, Object> seedController = (Controller<Void, Object>) controllers.get("__seed__");
        Context seed = new Context.Builder().build(seedController);
        seedController.accept(new Batch<>(seed, null));
        seedController.finish(null, seed);

        try {

            synchronized (lock) {
                while (controllers.values().stream().anyMatch(c -> c.workers.size() != c.done.size())) {
                    lock.wait();
                }
            }

            for (Controller<?, ?> controller : controllers.values()) {
                for (Worker worker : controller.workers) {
                    worker.interrupt();
                    worker.join();
                }
            }

            if (report) {
                reporter.interrupt();
                reporter.join();
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);  // TODO
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

    public class ControllerInterface {

        private final String controllerId;

        private ControllerInterface(String controllerId) {
            this.controllerId = controllerId;
        }

        public Optional<Controller<?, ?>> getController(String id) {
            if (id.equals("__maybe_next__")) {
                return resolveMaybeNext(controllerId).map(controllers::get);
            } else {
                Controller<?, ?> controller = controllers.get(id);
                if (controller == null) throw new IllegalArgumentException("No controller has id " + id);
                return Optional.of(controller);
            }
        }

        public Collection<Controller<?, ?>> getControllers() {
            return controllers.values();
        }

        public void signalOpen() {
            synchronized (lock) {
                open.add(controllerId);
                lock.notify();
            }
        }

        public void signalDone() {
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public interface Configurator {
        void configure(TransformerConfiguration configuration);
    }
}
