package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.controller.SetupControls;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class Open implements Transformer<Path, InputStream> {

    @Override
    public void setup(SetupControls controls) {
        controls.requireDownstreamSync = true;
    }

    @Override
    public void transform(Context context, Path in, Receiver<InputStream> out) throws Exception {
        try (InputStream inputStream = Files.newInputStream(in)) {
            Optional<Object> monitor = context.fetch("monitor").findFirst();
            if (monitor.isPresent()) {
                out.accept(context, new InputStreamWrapper(inputStream, (Write.Monitor) monitor.get()));
            } else {
                out.accept(context, inputStream);
            }
        }
    }

    private static final class InputStreamWrapper extends FilterInputStream {

        private final Write.Monitor monitor;

        protected InputStreamWrapper(InputStream inputStream, Write.Monitor monitor) {
            super(inputStream);
            this.monitor = monitor;
        }

        @Override
        public int read() throws IOException {
            int read;
            while ((read = super.read()) == -1 && !monitor.isClosed()) {
                try {
                    monitor.awaitClose(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException(e.getMessage());
                }
            }
            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read;
            while ((read = super.read(b, off, len)) == -1 && !monitor.isClosed()) {
                try {
                    monitor.awaitClose(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException(e.getMessage());
                }
            }
            return read;
        }
    }
}
