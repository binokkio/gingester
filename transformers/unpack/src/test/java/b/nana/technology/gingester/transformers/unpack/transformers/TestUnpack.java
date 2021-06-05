package b.nana.technology.gingester.transformers.unpack.transformers;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToString;
import b.nana.technology.gingester.transformers.base.transformers.path.Open;
import b.nana.technology.gingester.transformers.base.transformers.path.Search;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUnpack {

    @Test
    void testUnpack() throws IOException {

        Path testDir = Files.createTempDirectory("gingester-unpack-test-");
        Path testFile = testDir.resolve("test.tar.gz");
        Files.write(testFile, requireNonNull(getClass().getResourceAsStream("/test.tar.gz")).readAllBytes());

        Search.Parameters searchParameters = new Search.Parameters();
        searchParameters.root = testDir.toString();
        searchParameters.globs = new String[] { testFile.getFileName().toString() };
        Search search = new Search(searchParameters);
        Open open = new Open();
        Unpack unpack = new Unpack();
        ToString toString = new ToString();

        Queue<String> descriptions = new LinkedBlockingQueue<>();
        Queue<String> results = new LinkedBlockingQueue<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.link(search, open);
        gBuilder.link(open, unpack);
        gBuilder.link(unpack, toString);
        gBuilder.link(toString, (context, value) -> {
            descriptions.add(context.getDescription());
            results.add(value);
        });
        gBuilder.build().run();

        Files.delete(testFile);
        Files.delete(testDir);

        assertEquals(new ArrayList<>(descriptions), List.of(
                "test.tar.gz :: test.tar :: b.gz :: b",
                "test.tar.gz :: test.tar :: c",
                "test.tar.gz :: test.tar :: test.zip :: a"
        ));

        assertEquals(new ArrayList<>(results), List.of("Hello, World!\n", "Hello, World!\n", "Hello, World!\n"));
    }
}
