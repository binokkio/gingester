package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class ToPath extends Transformer<InputStream, Path> {

    private final String pathFormat;
    private final boolean emitEarly;
    private final int bufferSize;

    public ToPath(Parameters parameters) {
        super(parameters);
        pathFormat = parameters.path;
        emitEarly = parameters.emitEarly;
        bufferSize = parameters.bufferSize;
    }

    @Override
    protected void transform(Context context, InputStream input) throws Exception {

        String[] descriptions = context.getDescriptions();
        for (int i = 0; i < descriptions.length; i++) {
            if (descriptions[i] != null) {
                descriptions[i] = descriptions[i].replaceAll("[^a-zA-Z0-9-_.]", "_");  // TODO use static final pattern
            }
        }

        String pathString = String.format(pathFormat, (Object[]) descriptions);
        Path path = Paths.get(pathString);

        Path parent = path.getParent();
        if (!Files.isDirectory(path.getParent())) {
            Files.createDirectories(parent);
        }

        Context.Builder contextBuilder = context.extend(this).description(pathString);

        // TODO make options configurable
        try (OutputStream output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            if (emitEarly) {
                Monitor monitor = new Monitor();
                contextBuilder.attachment(monitor);
                emit(contextBuilder, path);
                write(input, output);
                output.close();
                monitor.close();
            } else {
                write(input, output);
                output.close();
                emit(contextBuilder, path);
            }
        }
    }

    private void write(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
    }

    public static class Parameters {

        public String path = ".";
        public boolean emitEarly;
        public int bufferSize = 8192;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String path) {
            this.path = path;
        }
    }

    public static class Monitor {

        private final AtomicBoolean closed = new AtomicBoolean();

        public boolean isClosed() {
            return closed.get();
        }

        public void awaitClose(long millis) throws InterruptedException {
            synchronized (closed) {
                if (!closed.get()) {
                    closed.wait(millis);
                }
            }
        }

        private void close() {
            synchronized (closed) {
                closed.set(true);
                closed.notifyAll();
            }
        }
    }
}
