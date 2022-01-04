package b.nana.technology.gingester.transformers.base.common.iostream;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamWrapper extends OutputStream {

    private OutputStream wrapped;

    public void wrap(OutputStream outputStream) {
        wrapped = outputStream;
    }

    @Override
    public void write(int i) throws IOException {
        wrapped.write(i);
    }

    @Override
    public void write(byte[] b) throws IOException {
        wrapped.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        wrapped.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        wrapped.flush();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }
}
