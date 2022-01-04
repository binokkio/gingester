package b.nana.technology.gingester.transformers.base.common.iostream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public final class Splitter {

    private final byte[] delimiter;
    private final PrefixInputStream source;
    private boolean peek;

    public Splitter(InputStream source, byte[] delimiter) {
        this.source = new PrefixInputStream(source);
        this.source.setMinimumBufferSize(delimiter.length);
        this.delimiter = delimiter;
    }

    /**
     * The InputStream from the previous call to `getNextInputStream()` must be fully read before calling this method!
     */
    public Optional<InputStream> getNextInputStream() throws IOException {

        if (peek) {
            byte[] peek = new byte[1];
            int read = source.read(peek);
            if (read == -1) return Optional.empty();
            source.prefix(peek);
        }

        peek = true;

        return Optional.of(new InputStream() {

            private boolean done;

            @Override
            public int read() throws IOException {
                if (done) return -1;
                int read = source.read();
                if (read != delimiter[0]) return read;
                return peek(1) ? -1 : read;
            }

            @Override
            public int read(byte[] destination, int offset, int length) throws IOException {
                if (done) return -1;
                int seen = 0;
                int read = source.read(destination, offset, length);
                for (int i = offset; i < offset + read; i++) {
                    if (destination[i] == delimiter[seen]) {
                        if (++seen == delimiter.length) {
                            source.copyPrefix(destination, i + 1, read - (i - offset) - 1);
                            peek = false;
                            done = true;
                            return i - offset - (seen - 1);
                        }
                    } else if (seen > 0) {
                        i -= seen;
                        seen = 0;
                    }
                }
                if (seen == 0) return read;
                return peek(seen) ? read - seen : read;
            }

            private boolean peek(int seen) throws IOException {
                byte[] buffer = source.getBuffer();
                int buffered = buffer(buffer, seen, delimiter.length - seen);
                if (buffered != delimiter.length - seen) {  // reached EOS before filling buffer, can't be a full delimiter
                    source.prefix(buffer, seen, buffered);
                    return false;
                } else {
                    for (int i = seen; i < delimiter.length; i++) {
                        if (buffer[i] != delimiter[i]) {
                            source.prefix(buffer, seen, buffered);
                            return false;
                        }
                    }
                    peek = false;
                    done = true;
                    return true;
                }
            }

            private int buffer(byte[] buffer, int offset, int length) throws IOException {
                int total = 0;
                while (total < length) {
                    int read = source.read(buffer, offset + total, length - total);
                    if (read == -1) return total;
                    total += read;
                }
                return total;
            }
        });
    }

    public InputStream getRemaining() {
        return source;
    }
}
