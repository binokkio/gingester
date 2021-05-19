package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestToInputStream {

    @Test
    void test() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-transformers-test-");
        Path helloWorld = Files.write(tempDir.resolve("hello-world.txt"), List.of("Hello, World!"));

        try {
            AtomicReference<String> result = new AtomicReference<>();
            ToInputStream toInputStream = new ToInputStream();
            Gingester gingester = new Gingester();
            gingester.seed(toInputStream, helloWorld);
            gingester.link(toInputStream, inputStream -> {
                try {
                    result.set(new String(inputStream.readAllBytes()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            gingester.run();
            assertEquals("Hello, World!\n", result.get());

        } finally {
            Files.delete(helloWorld);
            Files.delete(tempDir);
        }
    }
}
