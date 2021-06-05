package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestSearch {

    @Test
    void test() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-transformers-test-");
        Path helloWorld1 = Files.createFile(tempDir.resolve("hello-world-1.txt"));
        Path helloWorld2 = Files.createFile(tempDir.resolve("hello-world-2.txt"));
        Path byeWorld1 = Files.createFile(tempDir.resolve("bye-world-1.txt"));
        Path nested = Files.createDirectory(tempDir.resolve("nested"));
        Path helloWorld3 = Files.createFile(tempDir.resolve("nested/hello-world-3.txt"));
        Path byeWorld2 = Files.createFile(tempDir.resolve("bye-world-2.txt"));

        try {

            Queue<Path> results = new LinkedBlockingQueue<>();

            Search.Parameters fileSearchParameters = new Search.Parameters();
            fileSearchParameters.root = tempDir.toString();
            fileSearchParameters.globs = new String[] { "**hello*" };
            Search fileSearch = new Search(fileSearchParameters);

            Gingester.Builder gBuilder = Gingester.newBuilder();
            gBuilder.link(fileSearch, results::add);
            gBuilder.build().run();

            assertTrue(results.contains(helloWorld1));
            assertTrue(results.contains(helloWorld2));
            assertTrue(results.contains(helloWorld3));
            assertFalse(results.contains(byeWorld1));
            assertFalse(results.contains(byeWorld2));

        } finally {
            Files.delete(byeWorld2);
            Files.delete(helloWorld3);
            Files.delete(nested);
            Files.delete(byeWorld1);
            Files.delete(helloWorld2);
            Files.delete(helloWorld1);
            Files.delete(tempDir);
        }
    }
}
