package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamMonitor;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Pure
public final class ToInputStream implements Transformer<Path, InputStream> {

    private final FetchKey fetchMonitor = new FetchKey("monitor");

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void transform(Context context, Path in, Receiver<InputStream> out) throws Exception {
        try (InputStream inputStream = Files.newInputStream(in)) {
            Optional<Object> monitor = context.fetch(fetchMonitor);
            if (monitor.isPresent()) {
                out.accept(context, new InputStreamWrapper(inputStream, (OutputStreamMonitor) monitor.get()));
            } else {
                out.accept(context, inputStream);
            }
        }
    }

    private static final class InputStreamWrapper extends FilterInputStream {

        private final OutputStreamMonitor monitor;

        private InputStreamWrapper(InputStream inputStream, OutputStreamMonitor monitor) {
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
