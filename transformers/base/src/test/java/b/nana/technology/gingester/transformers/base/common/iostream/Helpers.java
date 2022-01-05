package b.nana.technology.gingester.transformers.base.common.iostream;

import java.io.IOException;
import java.io.InputStream;

final class Helpers {

    private Helpers() {}

    static String readAllBytesToString(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes());
    }

    static String readSingleBytesToString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int read;
        while ((read = inputStream.read()) != -1) {
            stringBuilder.append((char) read);
        }
        return stringBuilder.toString();
    }

    static String readChunksOfBytesToString(InputStream inputStream, int chunkSize) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        byte[] buffer = new byte[chunkSize];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            stringBuilder.append(new String(buffer, 0, read));
        }
        return stringBuilder.toString();
    }
}
