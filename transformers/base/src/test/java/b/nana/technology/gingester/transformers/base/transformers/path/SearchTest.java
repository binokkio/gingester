package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import b.nana.technology.gingester.core.template.TemplateParameters;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SearchTest {

    @Test
    void test() throws Exception {

        Queue<Path> result = new ArrayDeque<>();

        Path tempDir = Files.createTempDirectory("gingester-");
        Path target = tempDir.resolve("hello-world.txt");
        Files.write(target, "Hello, World!".getBytes(StandardCharsets.UTF_8));

        Search.Parameters parameters = new Search.Parameters();
        parameters.root = new TemplateParameters(tempDir.toString());
        parameters.globs = List.of(new TemplateParameters("*"));

        new Search(parameters).transform(Context.newTestContext(), null, (UniReceiver<Path>) result::add);

        assertEquals(1, result.size());
        assertEquals(target, result.remove());

        Files.delete(target);
        Files.delete(tempDir);
    }

    @Test
    void testMultipleGlobs() throws Exception {

        Set<Path> result = Collections.synchronizedSet(new HashSet<>());

        Path tempDir = Files.createTempDirectory("gingester-");

        Path a = tempDir.resolve("a.txt");
        Files.write(a, "Hello, World!".getBytes(StandardCharsets.UTF_8));

        Path b = tempDir.resolve("b.txt");
        Files.write(b, "Hello, World!".getBytes(StandardCharsets.UTF_8));

        Path c = tempDir.resolve("c.txt");
        Files.write(c, "Hello, World!".getBytes(StandardCharsets.UTF_8));

        Search.Parameters parameters = new Search.Parameters();
        parameters.root = new TemplateParameters(tempDir.toString());
        parameters.globs = List.of(new TemplateParameters("a.txt"), new TemplateParameters("c.txt"));

        new Search(parameters).transform(Context.newTestContext(), null, (UniReceiver<Path>) result::add);

        assertEquals(2, result.size());
        assertEquals(Set.of(a, c), result);

        Files.delete(a);
        Files.delete(b);
        Files.delete(c);
        Files.delete(tempDir);
    }

    @Test
    void testCliVariations() {
        new Gingester().cli("-t PathSearch '*'");
        new Gingester().cli("-t PathSearch '\"*\"'");
        new Gingester().cli("-t PathSearch \"'*'\"");
        new Gingester().cli("-t PathSearch ['hello']");
        new Gingester().cli("-t PathSearch [hello','world']");
        new Gingester().cli("-t PathSearch \"['hello', 'world']\"");
        new Gingester().cli("-t PathSearch '[\"hello\", \"world\"]'");
        new Gingester().cli("-t PathSearch {globs:'*'}");
        new Gingester().cli("-t PathSearch {globs:['hello','world']}");
        new Gingester().cli("-t PathSearch {globs:['hello',{template:'world'}]}");
        new Gingester().cli("-t PathSearch {globs:[{template:'*'}]}");
        new Gingester().cli("-t PathSearch {template:'*'}");
        new Gingester().cli("-t PathSearch ['hello',{template:'world'}]");
    }
}