package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonArrayToFilesTest {

    @Test
    void testJsonArrayToFiles() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-");

        FlowBuilder flowBuilder = new FlowBuilder().cli(
                getClass().getResource("/configurations/json-array-to-files.cli"),
                Map.of("tempDir", tempDir)
        );

        flowBuilder.run();

        Path message123 = tempDir.resolve("message-123.txt");
        assertTrue(Files.exists(message123));
        assertEquals("Hello, World 1!", Files.readString(message123));
        Files.delete(message123);

        Path message234 = tempDir.resolve("message-234.txt");
        assertTrue(Files.exists(message234));
        assertEquals("Hello, World 2!", Files.readString(message234));
        Files.delete(message234);

        Path message345 = tempDir.resolve("message-345.txt");
        assertTrue(Files.exists(message345));
        assertEquals("Hello, World 3!", Files.readString(message345));
        Files.delete(message345);

        Files.delete(tempDir);
    }
}
