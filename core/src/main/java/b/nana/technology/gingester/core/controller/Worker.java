package b.nana.technology.gingester.core.controller;

public class Worker extends Thread {

    private final Controller<?, ?> controller;

    public Worker(Controller<?, ?> controller) {
        this.controller = controller;
    }

    @Override
    public void run() {

    }
}
