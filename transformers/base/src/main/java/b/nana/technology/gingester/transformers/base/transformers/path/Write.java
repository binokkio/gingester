package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.controller.SetupControls;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
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

public class Write implements Transformer<InputStream, Path> {

    private final Context.Template pathTemplate;
    private final boolean mkdirs;
    private final StandardOpenOption[] openOptions;
    private final boolean emitEarly;
    private final int bufferSize;

    public Write(Parameters parameters) {
        pathTemplate = Context.newTemplate(parameters.path);
        mkdirs = parameters.mkdirs;
        openOptions = parameters.openOptions;
        emitEarly = parameters.emitEarly;
        bufferSize = parameters.bufferSize;
    }

    @Override
    public void setup(SetupControls controls) {
        if (emitEarly) {
//            setup.requireDownstreamAsync();
        }
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<Path> out) throws Exception {

        String pathString = pathTemplate.render(context);
        Path path = Paths.get(pathString);

        Path parent = path.getParent();
        if (mkdirs && parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        try (OutputStream output = Files.newOutputStream(path, openOptions)) {
            if (emitEarly) {
                Monitor monitor = new Monitor();
                out.accept(context.stash(Map.of(
                        "description", pathString,
                        "monitor", monitor
                )), path);
                write(in, output);
                output.close();
                monitor.close();
            } else {
                write(in, output);
                output.close();
                out.accept(context.stash(Map.of("description", pathString)), path);
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
