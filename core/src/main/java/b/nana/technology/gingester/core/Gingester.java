package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.controller.Controller;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.LinkedHashMap;

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
                new DriverInterface(),
                transformer,
                parameters
        );

        controllers.put(id, controller);
    }

    public void run() {
        controllers.values().forEach(Controller::initialize);
    }



    public class DriverInterface {
        private DriverInterface() {}
    }
}
