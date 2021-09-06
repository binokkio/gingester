package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.controller.Configuration;
import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.controller.Controller;
import b.nana.technology.gingester.core.controller.Worker;
import b.nana.technology.gingester.core.reporting.Reporter;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Seed;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/*
 * TODO adding 1 more step between `configuration` and `controllers` would allow
 *      all controllers to have final unmodifiable maps for their incoming and outgoing
 *      maps. Could be called `setup`.
 */

public final class Gingester {

    private final LinkedHashMap<String, Configuration> configurations = new LinkedHashMap<>();
    private final LinkedHashMap<String, Controller<?, ?>> controllers = new LinkedHashMap<>();
    private boolean report;

    public void report(Boolean report) {
        this.report = report != null && report;
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
        Configuration configuration = new Configuration();
        configurator.configure(configuration);
        add(configuration);
    }

    public void add(Configuration configuration) {
        String id = getId(configuration);
        configurations.put(id, configuration);
    }

    private String getId(Configuration configuration) {
        String id;
        if (configuration.getId() != null) {
            if (configurations.containsKey(configuration.getId())) {
                throw new IllegalArgumentException("Controller id " + configuration.getId() + " already in use");
            }
            id = configuration.getId();
        } else {
            id = configuration.getTransformer();
            int i = 1;
            while (configurations.containsKey(id)) {
                id = configuration.getTransformer() + '-' + i++;
            }
        }
        return id;
    }

    public void run() {

        if (configurations.isEmpty()) {
            throw new IllegalStateException("No transformers configured");
        }

        configurations.forEach((id, configuration) -> controllers.put(
                id, new Controller<>(configuration, new ControllerInterface(id))));

        controllers.values().forEach(Controller::initialize);

        Configuration seedControllerConfiguration = new Configuration();
        seedControllerConfiguration.transformer(new Seed());
        Set<Controller<?, ?>> noIncoming = new HashSet<>(controllers.values());
        controllers.values().stream().map(c -> c.outgoing.values()).forEach(noIncoming::removeAll);
        seedControllerConfiguration.links(noIncoming.stream().map(c -> c.id).collect(Collectors.toList()));
        Controller<Void, Object> seedController = new Controller<>(
                seedControllerConfiguration,
                new ControllerInterface("__seed__")
        );
        seedController.initialize();
        controllers.put("__seed__", seedController);

        controllers.values().forEach(Controller::discover);
        controllers.values().forEach(Controller::start);

        Context seed = new Context.Builder()
                .controller(seedController)
                .build();

        Reporter reporter = new Reporter(controllers.values());
        if (report) reporter.start();

        seedController.accept(new Batch<>(seed, null));
        seedController.finish(null, seed);

        try {
            for (Controller<?, ?> controller : controllers.values()) {
                for (Worker worker : controller.workers) {
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


    public class ControllerInterface {

        private final String controllerId;

        private ControllerInterface(String controllerId) {
            this.controllerId = controllerId;
        }

        public String getId() {
            return controllerId;
        }

        public Optional<Controller<?, ?>> getController(String id) {

            if (id.equals("__maybe_next__")) {
                boolean next = false;
                for (Map.Entry<String, Controller<?, ?>> idControllerEntry : controllers.entrySet()) {
                    if (next) {
                        return Optional.of(idControllerEntry.getValue());
                    } else if (idControllerEntry.getKey().equals(controllerId)) {
                        next = true;
                    }
                }
                return Optional.empty();
            }

            Controller<?, ?> controller = controllers.get(id);
            if (controller == null) throw new IllegalArgumentException("No controller has id " + id);
            return Optional.of(controller);
        }

        public Collection<Controller<?, ?>> getControllers() {
            return controllers.values();
        }

        public boolean hasNext() {
            boolean next = false;
            for (String id : configurations.keySet()) {
                if (next) {
                    return true;
                } else if (id.equals(controllerId)) {
                    next = true;
                }
            }
            return false;
        }
    }

    public interface Configurator {
        void configure(Configuration configuration);
    }
}
