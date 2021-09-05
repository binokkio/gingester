package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.controller.Configuration;
import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.controller.Controller;
import b.nana.technology.gingester.core.controller.Worker;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Seed;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class Gingester {

    private final LinkedHashMap<String, Controller<?, ?>> controllers = new LinkedHashMap<>();

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
        controllers.put(id, new Controller<>(
                configuration,
                new ControllerInterface(id)
        ));
    }

    private String getId(Configuration configuration) {
        String id;
        if (configuration.getId() != null) {
            if (controllers.containsKey(configuration.getId())) {
                throw new IllegalArgumentException("Controller id " + configuration.getId() + " already in use");
            }
            id = configuration.getId();
        } else {
            id = configuration.getTransformer();
            int i = 1;
            while (controllers.containsKey(id)) {
                id = configuration.getTransformer() + '-' + i++;
            }
        }
        return id;
    }

    public void run() {

        if (controllers.isEmpty()) {
            throw new IllegalStateException("No transformers");
        }

        controllers.values().forEach(Controller::initialize);
        controllers.values().forEach(Controller::discover);

        Configuration seedControllerConfiguration = new Configuration();
        seedControllerConfiguration.links(controllers.entrySet().stream()
                .filter(entry -> entry.getValue().incoming.isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()));

        Controller<Void, Object> seedController = new Controller<>(
                new Seed(),
                seedControllerConfiguration,
                new ControllerInterface("__seed__")
        );
        seedController.initialize();
        controllers.put("__seed__", seedController);

        // TODO start reporting thread

        controllers.values().forEach(Controller::start);

        Context seed = new Context.Builder()
                .controller(seedController)
                .build();

        seedController.accept(new Batch<>(seed, null));
        seedController.finish(null, seed);

        for (Controller<?, ?> controller : controllers.values()) {
            for (Worker worker : controller.workers) {
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);  // TODO
                }
            }
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
    }

    public interface Configurator {
        void configure(Configuration configuration);
    }
}
