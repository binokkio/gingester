package b.nana.technology.gingester;

import b.nana.technology.gingester.core.Gingester;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class PackTest {

    @Test
    void testPackIndividually() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-");

        Gingester gingester = new Gingester();

        gingester.configure(c -> c
                .transformer("String.Create")
                .jsonParameters("{template:'Hello, World!',count:1000}"));

        gingester.add("String.ToBytes");

        gingester.configure(c -> c
                .transformer("Pack")
                .syncs(Collections.singletonList("String.Create"))
                .parameters("hello.txt"));

        gingester.configure(c -> c
                .transformer("Path.Write")
                .parameters(tempDir.resolve("result-${description}.tar.gz").toString()));

        gingester.run();

        for (int i = 0; i < 1000; i++) {

            Path result = tempDir.resolve("result-" + i + ".tar.gz");
            assertTrue(Files.exists(result));

            TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(result)));
            TarArchiveEntry entry = tar.getNextTarEntry();
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

        Gingester gingester = new Gingester();

        gingester.configure(c -> c
                .transformer("String.Create")
                .jsonParameters("{template:'Hello, World!',count:1000}"));

        gingester.add("String.ToBytes");

        gingester.configure(c -> c
                .transformer("Pack")
                .syncs(Collections.singletonList("__seed__"))
                .parameters("hello-${description}.txt"));

        gingester.configure(c -> c
                .transformer("Path.Write")
                .parameters(tempDir.resolve("result.tar.gz").toString()));

        gingester.run();

        Path result = tempDir.resolve("result.tar.gz");
        assertTrue(Files.exists(result));

        TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(result)));

        for (int i = 0; i < 1000; i++) {

            TarArchiveEntry entry = tar.getNextTarEntry();
            assertNotNull(entry);
            assertEquals("hello-" + i + ".txt", entry.getName());

            String content = new String(tar.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Hello, World!", content);
        }

        Files.delete(result);
        Files.delete(tempDir);
    }
}
