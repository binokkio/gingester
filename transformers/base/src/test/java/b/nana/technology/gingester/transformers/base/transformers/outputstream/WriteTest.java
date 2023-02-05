package b.nana.technology.gingester.transformers.base.transformers.outputstream;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WriteTest {

    @Test
    void test() throws Exception {

        Path tempFile = Files.createTempFile("gingester-", ".txt");

        new FlowBuilder().cli(
                "-s tempFile " +
                "-t PathToOutputStream " +
                "-s logs " +
                "-t Write logs 'Writing to ${tempFile}' !newline " +
                "-t Close logs")
                .seed(tempFile)
                .run();

        assertEquals("Writing to " + tempFile, Files.readString(tempFile));

        Files.delete(tempFile);
    }
}