package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.controller.Controller;
import b.nana.technology.gingester.core.configuration.Parameters;
import b.nana.technology.gingester.core.controller.Worker;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Seed;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Gingester {

    private final LinkedHashMap<String, Controller<?, ?>> controllers = new LinkedHashMap<>();

    public String add(Parameters parameters) {
        String id = getId(parameters);
        controllers.put(id, new Controller<>(
                parameters,
                new ControllerInterface(id)
        ));
        return id;
    }

    public <T> String add(Consumer<T> consumer) {
        return add((Transformer<T, T>) (context, in, out) -> {
            consumer.accept(in);
            out.accept(context, in);
        });
    }

    public <T> String add(Consumer<T> consumer, Parameters parameters) {
        return add((Transformer<T, T>) (context, in, out) -> {
            consumer.accept(in);
            out.accept(context, in);
        }, parameters);
    }

    public String add(Transformer<?, ?> transformer) {
        return add(transformer, new Parameters());
    }

    public String add(Transformer<?, ?> transformer, Parameters parameters) {

        if (parameters.getTransformer() == null) {
            parameters.setTransformer(transformer.getClass().getSimpleName());
        }

        String id = getId(parameters);

        controllers.put(id, new Controller<>(
                transformer,
                parameters,
                new ControllerInterface(id)
        ));

        return id;
    }

    private String getId(Parameters parameters) {
        String id;
        if (parameters.getId() != null) {
            if (controllers.containsKey(parameters.getId())) {
                throw new IllegalArgumentException("Controller id " + parameters.getId() + " already in use");
            }
            id = parameters.getId();
        } else {
            id = parameters.getTransformer();
            int i = 1;
            while (controllers.containsKey(id)) {
                id = parameters.getTransformer() + '-' + i++;
            }
        }
        return id;
    }

    public void run() {

        controllers.values().forEach(Controller::initialize);
        controllers.values().forEach(Controller::discover);

        Parameters seedControllerParameters = new Parameters();
        seedControllerParameters.setLinks(controllers.entrySet().stream()
                .filter(entry -> entry.getValue().incoming.isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()));

        Controller<Void, Object> seedController = new Controller<>(
                new Seed(),
                seedControllerParameters,
                new ControllerInterface("__seed__")
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
}
