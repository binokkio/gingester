package b.nana.technology.gingester.transformers.unpack.packers;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public final class ZipPacker implements Packer {

    private final ZipArchiveOutputStream zip;

    public ZipPacker(OutputStream outputStream) {
        zip = new ZipArchiveOutputStream(outputStream);
    }

    @Override
    public void add(String name, byte[] bytes) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(name);
        entry.setSize(bytes.length);
        zip.putArchiveEntry(entry);
        zip.write(bytes);
        zip.closeArchiveEntry();
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }
}
