package b.nana.technology.gingester.transformers.base.common.iostream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OutputStreamWrapper extends OutputStream implements OutputStreamMonitor {

    private final AtomicBoolean closed = new AtomicBoolean();
    private OutputStream wrapped;

    public void wrap(OutputStream outputStream) {
        wrapped = outputStream;
    }

    @Override
    public void write(int i) throws IOException {
        wrapped.write(i);
    }

    @Override
    public void write(byte[] b) throws IOException {
        wrapped.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        wrapped.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        wrapped.flush();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
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
}
