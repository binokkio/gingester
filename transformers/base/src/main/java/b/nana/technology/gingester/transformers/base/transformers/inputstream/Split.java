package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Optional;

public class Split extends Transformer<InputStream, InputStream> {

    private final byte[] delimiter;

    public Split(Parameters parameters) {
        super(parameters);
        delimiter = parameters.delimiter.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void setup(Setup setup) {
        setup.requireDownstreamSync();
    }

    @Override
    protected void transform(Context context, InputStream input) throws Exception {
        Splitter splitter = new Splitter(input, delimiter);
        Optional<InputStream> split;
        long counter = 0;
        while ((split = splitter.getNextInputStream()).isPresent()) {
            emit(context.extend(this).description(++counter), split.get());
        }
    }

    public static class Parameters {

        public String delimiter;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String delimiter) {
            this.delimiter = delimiter;
        }
    }

    public static class Splitter {

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
                            if (destination[i] != delimiter[seen++]) {
                                seen = 0;
                                continue;
                            }
                            if (seen == delimiter.length) {
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

    public static class PrefixInputStream extends InputStream {

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
    }

    private static class Slice {

        private final byte[] bytes;
        private final int offset;
        private final int length;

        private Slice(byte[] bytes, int offset, int length) {
            this.bytes = bytes;
            this.offset = offset;
            this.length = length;
        }
    }
}
