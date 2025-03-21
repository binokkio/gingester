package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class PackTest {

    @Test
    void testPackIndividually() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-");

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-sft Repeat 1000 " +
                "-t StringToBytes " +
                "-stt Pack hello.txt -t Compress " +
                "-t PathWrite " + tempDir.resolve("result-${description}.tar.gz"));

        flowBuilder.run();

        for (int i = 0; i < 1000; i++) {

            Path result = tempDir.resolve("result-" + i + ".tar.gz");
            assertTrue(Files.exists(result));

            TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(result)));
            TarArchiveEntry entry = tar.getNextEntry();
            assertNotNull(entry);
            assertEquals("hello.txt", entry.getName());

            String content = new String(tar.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Hello, World!", content);

            Files.delete(result);
        }

        Files.delete(tempDir);
    }

    @Test
    void testPackTogether() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-");

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-sft StringDef 'Hello, World!' " +
                "-t Repeat 1000 " +
                "-t StringToBytes " +
                "-stt Pack hello-${description}.txt -t Compress " +
                "-t PathWrite " + tempDir.resolve("result.tar.gz"));

        flowBuilder.run();

        Path result = tempDir.resolve("result.tar.gz");
        assertTrue(Files.exists(result));

        TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(result)));

        for (int i = 0; i < 1000; i++) {

            TarArchiveEntry entry = tar.getNextEntry();
            assertNotNull(entry);
            assertEquals("hello-" + i + ".txt", entry.getName());

            String content = new String(tar.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Hello, World!", content);
        }

        Files.delete(result);
        Files.delete(tempDir);
    }

    @Test
    void testPackTogetherImplicitly() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-");

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Repeat 1000 " +
                "-t StringToBytes " +
                "-t Pack hello-${description}.txt -t Compress " +
                "-t PathWrite " + tempDir.resolve("result.tar.gz"));

        flowBuilder.run();

        Path result = tempDir.resolve("result.tar.gz");
        assertTrue(Files.exists(result));

        TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(result)));

        for (int i = 0; i < 1000; i++) {

            TarArchiveEntry entry = tar.getNextEntry();
            assertNotNull(entry);
            assertEquals("hello-" + i + ".txt", entry.getName());

            String content = new String(tar.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Hello, World!", content);
        }

        Files.delete(result);
        Files.delete(tempDir);
    }
}
