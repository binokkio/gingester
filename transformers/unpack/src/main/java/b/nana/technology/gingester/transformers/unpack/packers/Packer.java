package b.nana.technology.gingester.transformers.unpack.packers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;

public interface Packer {

    enum Type {
        JAR(JarPacker::new),
        TAR(TarPacker::new),
        ZIP(ZipPacker::new);

        private final Function<OutputStream, Packer> createArchive;

        Type(Function<OutputStream, Packer> createArchive) {
            this.createArchive = createArchive;
        }

        public Packer create(OutputStream outputStream) {
            return createArchive.apply(outputStream);
        }
    }

    void add(String name, byte[] bytes) throws IOException;
    void close() throws IOException;
}
