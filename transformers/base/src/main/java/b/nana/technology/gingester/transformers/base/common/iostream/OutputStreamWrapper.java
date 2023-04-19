package b.nana.technology.gingester.transformers.base.common.iostream;

import java.io.IOException;
import java.io.OutputStream;

public final class OutputStreamWrapper extends OutputStream implements OutputStreamMonitor {

    private final Object lock = new Object();
    private volatile boolean closed;
    private OutputStream destination;
    private Runnable onClose;

    public void wrap(OutputStream outputStream) {
        destination = outputStream;
    }

    @Override
    public void write(int i) throws IOException {
        requireDestination();
        destination.write(i);
    }

    @Override
    public void write(byte[] b) throws IOException {
        requireDestination();
        destination.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        requireDestination();
        destination.write(b, off, len);
    }

    private void requireDestination() {
        if (destination == null) {
            throw new IllegalStateException("OutputStream has no destination");
        }
    }

    @Override
    public void flush() throws IOException {
        if (destination != null) {
            destination.flush();
        }
    }

    @Override
    public void close() throws IOException {

        synchronized (lock) {
            if (closed) return;
            closed = true;
        }

        if (destination != null) destination.close();
        if (onClose != null) onClose.run();

        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void awaitClose(long millis) throws InterruptedException {
        synchronized (lock) {
            if (!closed) {
                lock.wait(millis);
            }
        }
    }

    public void onClose(Runnable onClose) {
        if (this.onClose != null) throw new IllegalStateException("onClose already set");
        this.onClose = onClose;
    }
}
