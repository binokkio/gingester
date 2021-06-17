package b.nana.technology.gingester.transformers.base.common.inputstream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class Splitter {

    private final byte[] delimiter;
    private final PrefixInputStream source;
    private int seen;  // number of bytes of the delimiter that have been seen
    private boolean peek = true;

    public Splitter(InputStream source, byte[] delimiter) {
        this.source = new PrefixInputStream(source);
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
            public int read(byte[] destination, int offset, int length) throws IOException {

                if (done) return -1;
                if (length == 0) return 0;  // TODO is this really necessary?

                int total = 0;
                int read;

                if (length < delimiter.length) {
                    byte[] temp = new byte[delimiter.length];
                    read = read(temp);
                    if (read > length) {
                        System.arraycopy(temp, 0, destination, offset, length);
                        source.prefix(delimiter, 0, seen);
                        source.prefix(temp, length, read - length);
                        seen = 0;
                        return length;
                    } else if (read == -1) {
                        return -1;
                    } else {
                        System.arraycopy(temp, 0, destination, offset, read);
                        return read;
                    }
                }

                while ((read = source.read(destination, offset + total, length - total)) != -1) {
                    total += read;
                    if (total >= delimiter.length) break;
                }

                if (total != 0) {
                    for (int i = offset; i < offset + total; i++) {
                        if (destination[i] == delimiter[seen]) {
                            if (++seen == delimiter.length) {
                                done = true;
                                peek = false;
                                int nextStart = i + 1;
                                int knownRemaining = total - (nextStart - offset);
                                if (knownRemaining > 0) {
                                    byte[] remaining = new byte[knownRemaining];
                                    System.arraycopy(destination, nextStart, remaining, 0, remaining.length);
                                    source.prefix(remaining);
                                }
                                total = i - offset - (seen - 1);
                                seen = 0;
                                return total > 0 ? total : -1;
                            }
                        } else if (seen > 0) {
                            i -= seen;  // restart one character past where we saw a delimiter start
                            seen = 0;
                        }
                    }
                    if (read == -1) {
                        seen = 0;
                        return total;
                    } else {
                        return total - seen;
                    }
                } else if (seen > 0) {
                    System.arraycopy(delimiter, 0, destination, offset, seen);
                    int moribund = seen;
                    seen = 0;
                    return moribund;
                } else {
                    return -1;
                }
            }

            @Override
            public int read() throws IOException {
                byte[] buffer = new byte[delimiter.length];
                int read = read(buffer);
                if (read == -1) return -1;
                if (read > 1) {
                    source.prefix(buffer, 1, read - 1);
                }
                return buffer[0];
            }
        });
    }
}
