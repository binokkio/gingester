package b.nana.technology.gingester.core.common;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

public class InputStreamReplicator {

    private final byte[] buffer;
    private final byte[] hash;
    private final Path tempFile;

    public InputStreamReplicator(InputStream inputStream, Path tempDir, int bufferSize, boolean calculateHash) throws IOException {

        byte[] buffer = inputStream.readNBytes(bufferSize);

        if (buffer.length < bufferSize) {
            this.buffer = buffer;
            this.hash = null;
            this.tempFile = null;
            return;
        }

        this.buffer = null;

        // recreate `inputStream` by combining the read and unread bytes
        inputStream = new SequenceInputStream(new ByteArrayInputStream(Arrays.copyOf(buffer, bufferSize)), inputStream);

        // maybe wrap `inputStream` in a digest input stream so we can read it while calculating a hash
        MessageDigest messageDigest = null;
        if (calculateHash) {
            try {
                messageDigest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            inputStream = new DigestInputStream(inputStream, messageDigest);
        }

        // write to disk
        tempFile = tempDir.resolve(Path.of(UUID.randomUUID().toString()));
        try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }

        // get hash
        hash = messageDigest != null ? messageDigest.digest() : null;
    }

    public InputStream replicate() {
        try {
            return tempFile != null ?
                    Files.newInputStream(tempFile) :
                    new ByteArrayInputStream(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        if (buffer != null) return Arrays.hashCode(buffer);
        if (hash != null) return Arrays.hashCode(hash);
        if (tempFile != null) return tempFile.getFileName().hashCode();
        throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof InputStreamReplicator other) {

            if (buffer != null && other.buffer != null)
                return Arrays.equals(buffer, other.buffer);

            if (hash != null && other.hash != null && !Arrays.equals(hash, other.hash))
                return false;

            try (
                    InputStream inputStream = replicate();
                    InputStream otherInputStream = other.replicate()
            ) {
                return IOUtils.contentEquals(inputStream, otherInputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    public void close() {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
