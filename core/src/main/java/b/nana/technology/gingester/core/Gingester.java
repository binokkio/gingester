package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.controller.Controller;
import b.nana.technology.gingester.core.controller.Worker;
import b.nana.technology.gingester.core.transformers.Seed;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Gingester {

    private final LinkedHashMap<String, Controller<?, ?>> controllers = new LinkedHashMap<>();

    public <T> String add(Consumer<T> consumer) {
        return add((Transformer<T, T>) (context, in, out) -> {
            consumer.accept(in);
            out.accept(context, in);
        });
    }

    public String add(Transformer<?, ?> transformer) {
        return add(transformer, new Controller.Parameters());
    }

    public String add(Transformer<?, ?> transformer, Controller.Parameters parameters) {

        String id;
        if (parameters.id != null) {
            if (controllers.containsKey(parameters.id)) {
                throw new IllegalArgumentException("Controller id " + parameters.id + " already in use");
            }
            id = parameters.id;
        } else {
            int i = 1;
            do {
                id = transformer.getClass().getSimpleName() + '-' + i++;
            } while (controllers.containsKey(id));
        }

        controllers.put(id, new Controller<>(
                id,
                new ControllerInterface(id),
                transformer,
                parameters
        ));

        return id;
    }

    public void run() {

        controllers.values().forEach(Controller::initialize);
        controllers.values().forEach(Controller::discover);

        Controller.Parameters seedControllerParameters = new Controller.Parameters();
        seedControllerParameters.links = controllers.entrySet().stream()
                .filter(entry -> entry.getValue().incoming.isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Controller<Void, Object> seedController = new Controller<>(
                "__seed__",
                new ControllerInterface("__seed__"),
                new Seed(),
                seedControllerParameters
        );
        seedController.initialize();
        controllers.put("__seed__", seedController);

        // TODO start reporting thread

        controllers.values().forEach(Controller::start);

        Context seed = Context.newSeed(seedController);
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
}
