package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.controller.Controller;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Seed;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Gingester {

    private final LinkedHashMap<String, Controller<?, ?>> controllers = new LinkedHashMap<>();

    public void add(Transformer<?, ?> transformer) {
        add(transformer, new Controller.Parameters());
    }

    public void add(Transformer<?, ?> transformer, Controller.Parameters parameters) {

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

        Controller<?, ?> controller = new Controller<>(
                id,
                new ControllerInterface(id),
                transformer,
                parameters
        );

        controllers.put(id, controller);
    }

    public void run() {

        controllers.values().forEach(Controller::initialize);

        Controller.Parameters seedControllerParameters = new Controller.Parameters();
        seedControllerParameters.links = controllers.entrySet().stream()
                .filter(entry -> entry.getValue().outgoing.isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        controllers.put("__seed__", new Controller<>(
                "__seed__",
                new ControllerInterface("__seed__"),
                new Seed(),
                seedControllerParameters
        ));
        controllers.get("__seed__").initialize();

        controllers.values().forEach(Controller::discover);
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
    }
}
