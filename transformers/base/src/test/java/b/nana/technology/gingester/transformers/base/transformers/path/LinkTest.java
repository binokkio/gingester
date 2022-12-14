package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LinkTest {

    @Test
    void testLink() throws Exception {

        Path tempDir = Files.createTempDirectory("gingester-");
        Path original = tempDir.resolve("hello-world.txt");
        Path link = tempDir.resolve("hello-linked-world.txt");

        Files.write(original, "Hello, World!".getBytes(StandardCharsets.UTF_8));

        AtomicReference<Path> result = new AtomicReference<>();

        new FlowBuilder()
                .cli("-t PathDef " + original + " -t PathLink " + link)
                .add(result::set)
                .run();

        assertEquals(link, result.get());
        assertEquals("Hello, World!", Files.readString(result.get()));

        Files.delete(link);
        Files.delete(original);
        Files.delete(tempDir);
    }
}