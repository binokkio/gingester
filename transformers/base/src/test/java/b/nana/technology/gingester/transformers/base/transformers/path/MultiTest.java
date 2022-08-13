package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultiTest {

    @Test
    void test() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-");
        ArrayDeque<Path> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Dir:Repeat 6 " +
                "-t File:Repeat 6 " +
                "-t PathWrite [=tempDir]/${Dir.description}/${File.description}.txt " +
                "-t OnFinish " +
                "-t Repeat 2 " +
                "-t PathDef [=tempDir]/${description} " +
                "-t PathDelete true " +
                "-t OnFinish " +
                "-t Repeat 4 " +
                "-t PathDef [=tempDir]/${description} " +
                "-t PathFilterExistsIn " +
                "-t PathDelete true " +
                "-t OnFinish " +
                "-t PathDef [=tempDir] " +
                "-t PathDelete true",
                Map.of("tempDir", tempDir))
                .attach(results::add, "PathFilterExistsIn")
                .run();

        assertFalse(Files.exists(tempDir));
        assertEquals(2, results.size());
        assertTrue(results.contains(tempDir.resolve("2")));
        assertTrue(results.contains(tempDir.resolve("3")));
    }
}
