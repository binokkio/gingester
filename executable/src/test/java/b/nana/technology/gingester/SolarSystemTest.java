package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class SolarSystemTest {

    @Test
    void test() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-");
        ArrayDeque<Path> paths = new ArrayDeque<>();
        AtomicReference<Path> planetsPath = new AtomicReference<>();
        AtomicReference<Path> resultsPath = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli(getClass().getResource("/configurations/solar-system.cli"), Map.of(
                "resource", "/data/solar-system.tar.gz",
                "workDir", tempDir
        ));
        flowBuilder.addTo(paths::add, "UnpackedPaths");
        flowBuilder.addTo(planetsPath::set, "PathSearch");
        flowBuilder.addTo(resultsPath::set, "PathWrite");
        flowBuilder.run();

        assertEquals(tempDir.resolve("solar-system.tar/planets.csv"), planetsPath.get());

        String result = new String(new GZIPInputStream(Files.newInputStream(resultsPath.get())).readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(result.contains("{\"name\":\"Moon\",\"planet\":\"Earth\"}"));

        // clean up
        for (Path path : paths) {
            Files.delete(path);
        }

        Files.delete(tempDir.resolve("solar-system.tar"));
        Files.delete(resultsPath.get());
        Files.delete(tempDir);
    }
}
