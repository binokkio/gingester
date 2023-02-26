package b.nana.technology.gingester.transformers.unpack.packers;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public final class TarPacker implements Packer {

    private final TarArchiveOutputStream tar;

    public TarPacker(OutputStream outputStream) {
        tar = new TarArchiveOutputStream(outputStream);
    }

    @Override
    public void add(String name, byte[] bytes) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(bytes.length);
        tar.putArchiveEntry(entry);
        tar.write(bytes);
        tar.closeArchiveEntry();
    }

    @Override
    public void close() throws IOException {
        tar.close();
    }
}
