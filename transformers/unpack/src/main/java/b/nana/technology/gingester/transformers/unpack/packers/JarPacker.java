package b.nana.technology.gingester.transformers.unpack.packers;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public final class JarPacker implements Packer {

    private final JarArchiveOutputStream jar;

    public JarPacker(OutputStream outputStream) {
        jar = new JarArchiveOutputStream(outputStream);
    }

    @Override
    public void add(String name, byte[] bytes) throws IOException {
        JarArchiveEntry entry = new JarArchiveEntry(name);
        entry.setSize(bytes.length);
        jar.putArchiveEntry(entry);
        jar.write(bytes);
        jar.closeArchiveEntry();
    }

    @Override
    public void close() throws IOException {
        jar.close();
    }
}
