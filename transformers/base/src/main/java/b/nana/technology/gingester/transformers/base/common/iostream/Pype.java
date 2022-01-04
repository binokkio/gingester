package b.nana.technology.gingester.transformers.base.common.iostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Quick alternative to the standard PipedOutputStream-0.11.1 combination.
 */
public class Pype extends InputStream {

    private static final byte[] EOS_SENTINEL = new byte[0];
    private final byte[] delimiter;
    private final ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(100);  // TODO
    private byte[] next;
    private byte[] current = new byte[0];
    private int pointer;
    private boolean eos;

    public Pype() {
        this(new byte[0]);
    }

    public Pype(byte[] delimiter) {
        this.delimiter = delimiter;
    }

    public void add(byte[] bytes) throws InterruptedException {
        if (bytes.length > 0) queue.put(bytes);
    }

    public void addCloseSentinel() throws InterruptedException {
        queue.put(EOS_SENTINEL);
    }

    @Override
    public int read() throws IOException {
        if (eos) return -1;
        if (pointer == current.length) loadNext();
        if (eos) return -1;
        return current[pointer++];
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (eos) return -1;
        if (pointer == current.length) loadNext();
        if (eos) return -1;
        int read = Math.min(len, current.length - pointer);
        System.arraycopy(current, pointer, b, off, read);
        pointer += read;
        return read;
    }

    private void loadNext() throws InterruptedIOException {
        try {
            if (current == delimiter) {
                current = next;
            } else {
                next = queue.take();
                if (next == EOS_SENTINEL) eos = true;
                else if (delimiter.length > 0 && current.length > 0) current = delimiter;
                else current = next;
            }
            pointer = 0;
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.getMessage());
        }
    }
}
