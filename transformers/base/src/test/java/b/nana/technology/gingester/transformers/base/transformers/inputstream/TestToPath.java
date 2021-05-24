package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.transformers.string.Generate;
import b.nana.technology.gingester.transformers.base.transformers.string.ToInputStream;
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

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.seed(toPath, new ByteArrayInputStream(write.getBytes(StandardCharsets.UTF_8)));
        gBuilder.build().run();

        String read = Files.readString(tempFile);
        assertEquals(write, read);

        Files.delete(tempFile);
        Files.delete(tempDirectory);
    }

    @Test
    void testWithFormat() throws IOException {

        Generate.Parameters generateParameters = new Generate.Parameters();
        generateParameters.payload = "Hello, World!";
        generateParameters.count = 3;
        Generate generate = new Generate(generateParameters);

        ToInputStream stringToInputStream = new ToInputStream();

        Path tempDirectory = Files.createTempDirectory("gingester-inputstream-test-to-path-");
        ToPath toPath = new ToPath(new ToPath.Parameters(tempDirectory.resolve("test-{generate-counter}.txt").toString()));

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.link(generate, stringToInputStream);
        gBuilder.link(stringToInputStream, toPath);
        gBuilder.build().run();

        assertEquals("Hello, World!", Files.readString(tempDirectory.resolve("test-1.txt")));
        assertEquals("Hello, World!", Files.readString(tempDirectory.resolve("test-2.txt")));
        assertEquals("Hello, World!", Files.readString(tempDirectory.resolve("test-3.txt")));

        Files.delete(tempDirectory.resolve("test-1.txt"));
        Files.delete(tempDirectory.resolve("test-2.txt"));
        Files.delete(tempDirectory.resolve("test-3.txt"));
        Files.delete(tempDirectory);
    }
}
