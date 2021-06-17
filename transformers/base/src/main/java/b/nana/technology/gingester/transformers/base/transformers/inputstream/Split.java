package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        Optional<InputStream> optionalSplit;
        while ((optionalSplit = splitter.getNextInputStream()).isPresent()) {
            InputStream split = optionalSplit.get();
            emit(context, optionalSplit.get());
            split.transferTo(OutputStream.nullOutputStream());
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
        private InputStream source;
        private int seen;  // number of bytes of the delimiter that have been seen
        private int knownRemaining;

        public Splitter(InputStream source, byte[] delimiter) {
            this.source = source;
            this.delimiter = delimiter;
        }

        public Optional<InputStream> getNextInputStream() throws IOException {

            if (knownRemaining == 0) {
                byte[] peek = new byte[1];
                int read = source.read(peek);
                if (read == -1) return Optional.empty();
                source = new SequenceInputStream(new ByteArrayInputStream(peek), source);
            }

            knownRemaining = 0;

            return Optional.of(new InputStream() {

                private boolean done;

                @Override
                public int read(byte[] buffer, int offset, int length) throws IOException {

                    if (done) return -1;

                    if (length < delimiter.length) {
                        throw new IllegalArgumentException("Length must be at least delimiter.length");
                    }

                    int total = 0;
                    int read;
                    while ((read = source.read(buffer, offset + total, length - total)) != -1) {
                        total += read;
                        if (total == length) break;  // TODO move to while condition?
                    }

                    if (total != 0) {
                        for (int i = offset; i < offset + total; i++) {
                            if (buffer[i] != delimiter[seen++]) {
                                seen = 0;
                                continue;
                            }
                            if (seen == delimiter.length) {
                                done = true;
                                int nextStart = i + 1;
                                knownRemaining = total - (nextStart - offset);
                                byte[] remaining = new byte[knownRemaining];
                                System.arraycopy(buffer, nextStart, remaining, 0, remaining.length);
                                source = new SequenceInputStream(new ByteArrayInputStream(remaining), source);
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
                        source = new SequenceInputStream(
                                new ByteArrayInputStream(buffer, 1, read - 1),
                                source
                        );
                    }
                    return buffer[0];
                }
            });
        }
    }
}
