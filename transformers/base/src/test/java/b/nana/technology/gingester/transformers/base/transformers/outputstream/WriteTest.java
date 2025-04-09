package b.nana.technology.gingester.transformers.base.transformers.outputstream;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WriteTest {

    @Test
    void test() throws Exception {

        Path tempDir = Files.createTempDirectory("gingester-");

        new FlowBuilder().cli(
                "-t PathDef '[=tempDir]/dir-to-be-made/logs.txt' " +
                "-s tempFile " +
                "-t PathToOutputStream " +
                "-s logs " +
                "-t Write logs 'Writing to ${tempFile}' !newline " +
                "-t Close logs", Map.of("tempDir", tempDir))
                .run();

        Path expected = tempDir.resolve("dir-to-be-made/logs.txt");

        assertEquals("Writing to " + expected, Files.readString(expected));

        Files.delete(tempDir.resolve("dir-to-be-made/logs.txt"));
        Files.delete(tempDir.resolve("dir-to-be-made"));
        Files.delete(tempDir);
    }
}