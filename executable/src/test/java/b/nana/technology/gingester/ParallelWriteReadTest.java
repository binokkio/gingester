package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ParallelWriteReadTest {

    @Test
    void testParallelWriteRead() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-");

        ArrayDeque<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t Repeat 2 " +
                "-t Cycle '[\"Hello, World!\", \"Bye, World!\"]' " +
                "-t ObjectToString " +
                "-s message " +
                "-t Repeat 1000 " +
                "-t StringDef '${description}' " +
                "-sft GroupByEquals " +
                "-f message " +
                "-stt InputStreamJoin " +
                "-t Compress " +
                "-t PathWrite '[=tempDir]/result-${groupKey}.txt.gz' " +
                "-fg " +
                "-s description " +
                "-t Unpack " +
                "-t InputStreamToString",
                Map.of("tempDir", tempDir))
                .add(results::add)
                .run();

        assertEquals(1000, results.size());

        String expectedContent = "Hello, World!\nBye, World!";

        for (int i = 0; i < 1000; i++) {

            String flowContent = results.remove();
            assertEquals(expectedContent, flowContent);

            Path file = tempDir.resolve("result-" + i + ".txt.gz");
            String fileContent = new String(new GZIPInputStream(Files.newInputStream(file)).readAllBytes());
            assertEquals(expectedContent, fileContent);

            Files.delete(file);
        }

        Files.delete(tempDir);
    }

    @Test
    void testParallelWriteReadConfiguredLineByLine() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-");

        ArrayDeque<String> results = new ArrayDeque<>();

        new FlowBuilder()
                .cli("-t Repeat 2")
                .cli("-t Cycle '[\"Hello, World!\", \"Bye, World!\"]'")
                .cli("-t ObjectToString")
                .cli("-s message")
                .cli("-t Repeat 1000")
                .cli("-t StringDef '${description}'")
                .cli("-sft GroupByEquals")
                .cli("-f message")
                .cli("-stt InputStreamJoin")
                .cli("-t Compress")
                .cli("-t PathWrite '[=tempDir]/result-${groupKey}.txt.gz'", Map.of("tempDir", tempDir))
                .cli("-fg")
                .cli("-s description")
                .cli("-t Unpack")
                .cli("-t InputStreamToString")
                .add(results::add)
                .run();

        assertEquals(1000, results.size());

        String expectedContent = "Hello, World!\nBye, World!";

        for (int i = 0; i < 1000; i++) {

            String flowContent = results.remove();
            assertEquals(expectedContent, flowContent);

            Path file = tempDir.resolve("result-" + i + ".txt.gz");
            String fileContent = new String(new GZIPInputStream(Files.newInputStream(file)).readAllBytes());
            assertEquals(expectedContent, fileContent);

            Files.delete(file);
        }

        Files.delete(tempDir);
    }
}
