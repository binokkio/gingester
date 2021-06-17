package b.nana.technology.gingester.transformers.base.common.inputstream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;

public class PrefixInputStream extends InputStream {

    private final InputStream source;
    private final ArrayDeque<Slice> prefixes = new ArrayDeque<>();
    private int prefixRemaining;

    public PrefixInputStream(InputStream source) {
        this.source = source;
    }

    public void prefix(byte[] prefix) {
        prefix(prefix, 0, prefix.length);
    }

    public void prefix(byte[] prefix, int offset, int length) {
        if (length > 0) {
            prefixes.addFirst(new Slice(prefix, offset, length));
            prefixRemaining = length;
        }
    }

    @Override
    public int read() throws IOException {
        if (prefixRemaining > 1) {
            Slice prefix = prefixes.getFirst();
            return prefix.bytes[prefix.offset + prefix.length - prefixRemaining--];
        } else if (prefixRemaining == 1) {
            Slice prefix = prefixes.remove();
            byte read = prefix.bytes[prefix.offset + prefix.length - 1];
            prefixRemaining = prefixes.isEmpty() ? 0 : prefixes.peek().length;
            return read;
        } else {
            return source.read();
        }
    }

    @Override
    public int read(byte[] destination, int offset, int length) throws IOException {
        int remaining = length;
        while (prefixRemaining > 0) {
            if (remaining >= prefixRemaining) {
                Slice prefix = prefixes.remove();
                System.arraycopy(prefix.bytes, prefix.offset + prefix.length - prefixRemaining, destination, offset + length - remaining, prefixRemaining);
                remaining -= prefixRemaining;
                prefixRemaining = prefixes.isEmpty() ? 0 : prefixes.peek().length;
                if (remaining == 0) return length;
            } else {
                Slice prefix = prefixes.getFirst();
                System.arraycopy(prefix.bytes, prefix.offset + prefix.length - prefixRemaining, destination, offset + length - remaining, remaining);
                prefixRemaining -= remaining;
                return length;
            }
        }
        int read = source.read(destination, offset + length - remaining, remaining);
        if (remaining == length) {
            return read;
        } else if (read == -1) {
            return length - remaining;
        } else {
            return length - remaining + read;
        }
    }

    private static class Slice {

        final byte[] bytes;
        final int offset;
        final int length;

        public Slice(byte[] bytes, int offset, int length) {
            this.bytes = bytes;
            this.offset = offset;
            this.length = length;
        }
    }
}
