package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.controller.Controller;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Phaser;

public interface ControllerInterface {
    Phaser getPhaser();

    Collection<Controller<?, ?>> getControllers();

    Optional<Controller<?, ?>> getController(String id);

    boolean isDebugModeEnabled();

    boolean isExceptionHandler();

    boolean isStopping();
}
