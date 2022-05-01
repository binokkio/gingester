package b.nana.technology.gingester.transformers.base.common.iostream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OutputStreamWrapper extends OutputStream implements OutputStreamMonitor {

    private final AtomicBoolean closed = new AtomicBoolean();
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

        if (destination != null) destination.close();
        if (onClose != null) onClose.run();

        synchronized (closed) {
            closed.set(true);
            closed.notifyAll();
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void awaitClose(long millis) throws InterruptedException {
        synchronized (closed) {
            if (!closed.get()) {
                closed.wait(millis);
            }
        }
    }

    public void onClose(Runnable onClose) {
        if (this.onClose != null) throw new IllegalStateException("onClose already set");
        this.onClose = onClose;
    }
}
