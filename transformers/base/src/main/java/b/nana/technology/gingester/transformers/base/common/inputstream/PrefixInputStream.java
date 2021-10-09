package b.nana.technology.gingester.transformers.base.common.inputstream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;

public final class PrefixInputStream extends InputStream {

    private final InputStream source;
    private final ArrayDeque<Slice> prefixes = new ArrayDeque<>();
    private int prefixRemaining;
    private int bufferSize = 8192;
    private byte[] recycle;

    public PrefixInputStream(InputStream source) {
        this.source = source;
    }

    public void setMinimumBufferSize(int minimumBufferSize) {
        bufferSize = Math.max(minimumBufferSize, bufferSize);
        if (recycle != null && recycle.length < bufferSize) recycle = null;
    }

    public void prefix(byte[] prefix) {
        prefix(prefix, 0, prefix.length);
    }

    public void prefix(byte[] prefix, int offset, int length) {
        if (length > 0) {
            if (!prefixes.isEmpty()) {
                prefixes.getFirst().trimStart(prefixRemaining);
            }
            prefixes.addFirst(new Slice(prefix, offset, length));
            prefixRemaining = length;
        }
    }

    public void copyPrefix(byte[] source, int offset, int length) {
        byte[] prefix;
        if (source.length > bufferSize) {
            setMinimumBufferSize(source.length);
            prefix = new byte[bufferSize];
        } else if (recycle != null) {
            prefix = recycle;
            recycle = null;
        } else {
            prefix = new byte[bufferSize];
        }
        System.arraycopy(source, offset, prefix, 0, length);
        prefix(prefix, 0, length);
    }

    @Override
    public int read() throws IOException {
        if (prefixRemaining > 1) {
            Slice prefix = prefixes.getFirst();
            return prefix.bytes[prefix.offset + prefix.length - prefixRemaining--];
        } else if (prefixRemaining == 1) {
            Slice prefix = prefixes.remove();
            if (prefix.length >= bufferSize) recycle = prefix.bytes;
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
                if (prefix.length >= bufferSize) recycle = prefix.bytes;
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

    public byte[] getBuffer() {
        if (recycle != null) {
            byte[] moribund = recycle;
            recycle = null;
            return moribund;
        } else {
            return new byte[bufferSize];
        }
    }

    private static class Slice {

        final byte[] bytes;
        int offset;
        int length;

        Slice(byte[] bytes, int offset, int length) {
            this.bytes = bytes;
            this.offset = offset;
            this.length = length;
        }

        void trimStart(int newLength) {
            offset += length - newLength;
            length = newLength;
        }
    }
}
