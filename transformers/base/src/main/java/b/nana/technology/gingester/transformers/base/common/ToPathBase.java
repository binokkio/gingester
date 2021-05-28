package b.nana.technology.gingester.transformers.base.common;

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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public abstract class ToPathBase<I> extends Transformer<I, Path> {

    private static final Pattern SANITIZER = Pattern.compile("[^a-zA-Z0-9-_.]");

    protected abstract InputStream toInputStream(I input);

    private final Context.StringFormat pathFormat;
    private final boolean mkdirs;
    private final StandardOpenOption[] openOptions;
    private final boolean emitEarly;
    private final int bufferSize;

    public ToPathBase(Parameters parameters) {
        super(parameters);
        pathFormat = new Context.StringFormat(parameters.path, s -> SANITIZER.matcher(s).replaceAll("_"), true);
        mkdirs = parameters.mkdirs;
        openOptions = parameters.openOptions;
        emitEarly = parameters.emitEarly;
        bufferSize = parameters.bufferSize;
    }

    @Override
    protected void setup(Setup setup) {
        if (emitEarly) {
            setup.requireDownstreamAsync();
        }
    }

    @Override
    protected void transform(Context context, I input) throws Exception {

        String pathString = pathFormat.format(context);
        Path path = Paths.get(pathString);

        Path parent = path.getParent();
        if (mkdirs && !Files.exists(path.getParent())) {
            Files.createDirectories(parent);
        }

        Context.Builder contextBuilder = context.extend(this).description(pathString);

        // TODO make options configurable
        try (OutputStream output = Files.newOutputStream(path, openOptions)) {
            if (emitEarly) {
                Monitor monitor = new Monitor();
                contextBuilder.stash(Map.of("monitor", monitor));
                emit(contextBuilder, path);
                write(toInputStream(input), output);
                output.close();
                monitor.close();
            } else {
                write(toInputStream(input), output);
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

        public String path;
        public boolean mkdirs = true;
        public StandardOpenOption[] openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE_NEW };
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
