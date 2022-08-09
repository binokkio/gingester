package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.configuration.ControllerConfiguration;

import java.util.LinkedHashMap;

public interface ControllerConfigurationInterface {
    LinkedHashMap<String, ControllerConfiguration<?, ?>> getControllers();
}
