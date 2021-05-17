package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestToPath {

    @Test
    void test() throws IOException {

        Path tempDirectory = Files.createTempDirectory("gingester-inputstream-test-to-path-");
        Path tempFile = tempDirectory.resolve("test");
        ToPath toPath = new ToPath(new ToPath.Parameters(tempFile.toString()));
        String write = "Hello, World!";

        Gingester gingester = new Gingester();
        gingester.seed(toPath, new ByteArrayInputStream(write.getBytes(StandardCharsets.UTF_8)));
        gingester.run();

        String read = Files.readString(tempFile);
        assertEquals(write, read);

        Files.delete(tempFile);
        Files.delete(tempDirectory);
    }
}
