package b.nana.technology.gingester.core.common;

import java.io.IOException;
import java.io.InputStream;

public class Util {

    private Util() {}

    public static byte[] readExactly(InputStream inputStream, int length) throws IOException {
        byte[] target = new byte[length];
        readExactly(inputStream, target);
        return target;
    }

    public static void readExactly(InputStream inputStream, byte[] target) throws IOException {
        int offset;
        if ((offset = inputStream.read(target)) == 16) return;
        int read;
        while ((read = inputStream.read(target, offset, target.length - offset)) != -1) {
            offset += read;
            if (offset == target.length) break;
        }
        if (offset != target.length) throw new IOException("Failed to read exactly " + target.length + " bytes");
    }
}
