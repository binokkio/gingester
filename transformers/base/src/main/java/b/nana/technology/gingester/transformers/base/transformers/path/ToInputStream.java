package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToPath;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ToInputStream extends Transformer<Path, InputStream> {

    @Override
    protected void setup(Setup setup) {
        setup.syncOutputs();
    }

    @Override
    protected void transform(Context context, Path input) throws IOException, InterruptedException {
        try (InputStream inputStream = Files.newInputStream(input)) {
            List<Object> attachments = context.getAttachments(ToPath.class);
            if (!attachments.isEmpty()) {
                ToPath.Monitor monitor = (ToPath.Monitor) attachments.get(0);
                emit(context, new InputStreamWrapper(inputStream, monitor));
            } else {
                emit(context, inputStream);
            }
        }
    }

    private static final class InputStreamWrapper extends FilterInputStream {

        private final ToPath.Monitor monitor;

        protected InputStreamWrapper(InputStream inputStream, ToPath.Monitor monitor) {
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
