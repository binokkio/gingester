package b.nana.technology.gingester.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SimpleWorker {

    private static final Runnable STOP_SENTINEL = () -> {};

    private final Thread thread = new Thread(this::run);
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    public void start() {
        thread.start();
    }

    public void add(Runnable runnable) {
        queue.add(runnable);
    }

    public void stop() {
        queue.add(STOP_SENTINEL);
    }

    private void run() {

        while (true) {

            Runnable runnable;

            try {
                runnable = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;  // TODO
            }

            if (runnable == STOP_SENTINEL) {
                return;  // TODO
            }

            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();  // TODO
            }
        }
    }
}
