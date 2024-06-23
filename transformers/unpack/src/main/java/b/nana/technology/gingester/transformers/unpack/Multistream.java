package b.nana.technology.gingester.transformers.unpack;

import org.apache.commons.compress.compressors.CompressorInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public final class Multistream extends CompressorInputStream {

    private final PushbackInputStream inputStream;
    private final MemberStreamConstructor constructor;

    private InputStream current;

    public Multistream(InputStream inputStream, MemberStreamConstructor constructor) throws IOException {

        this.inputStream = new PushbackInputStream(inputStream);
        this.constructor = constructor;

        current = constructor.construct(inputStream);
    }

    @Override
    public int read() throws IOException {

        int read = current.read();
        if (read != -1) return read;

        read = inputStream.read();
        if (read == -1) return read;
        inputStream.unread(read);

        current = constructor.construct(inputStream);

        return read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {

        int read = current.read(b, off, len);
        if (read != -1) return read;

        read = inputStream.read();
        if (read == -1) return read;
        inputStream.unread(read);

        current = constructor.construct(inputStream);

        return read(b, off, len);
    }

    public interface MemberStreamConstructor {
        InputStream construct(InputStream inputStream) throws IOException;
    }
}
