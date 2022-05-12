package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.BiReceiver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnpackTest {

    @Test
    void testUnpackTestTarGz() throws Exception {

        Context context = Context.newTestContext()
                .stash(Map.of("description", "/tmp/test.tar.gz"))
                .buildForTesting();

        Queue<String> results = new ArrayDeque<>();

        Unpack unpack = new Unpack(new Unpack.Parameters());
        unpack.transform(
                context,
                getClass().getResourceAsStream("/test.tar.gz"),
                (BiReceiver<InputStream>) (c, o) -> {
                    try {
                        results.add(c.fetchReverse(new FetchKey("description")).map(s ->
                                (String) s).collect(Collectors.joining(" :: ")) + " -> " + new String(o.readAllBytes()).trim());
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
        );

        assertEquals(5, results.size());
        assertEquals("/tmp/test.tar.gz :: test.tar :: b.gz :: b -> Hello, World!", results.remove());
        assertEquals("/tmp/test.tar.gz :: test.tar :: c -> Hello, World!", results.remove());
        assertEquals("/tmp/test.tar.gz :: test.tar :: dir/d.bz2 :: d -> Hello, World", results.remove());
        assertEquals("/tmp/test.tar.gz :: test.tar :: f.7z :: e -> Hello, 7zip World!", results.remove());
        assertEquals("/tmp/test.tar.gz :: test.tar :: test.zip :: a -> Hello, World!", results.remove());
    }

    @Test
    void testUnpackTestRar() throws Exception {

        Context context = Context.newTestContext()
                .stash(Map.of("description", "/tmp/test.rar"))
                .buildForTesting();

        Queue<String> results = new ArrayDeque<>();

        Unpack unpack = new Unpack(new Unpack.Parameters());
        unpack.transform(
                context,
                getClass().getResourceAsStream("/test.rar"),
                (BiReceiver<InputStream>) (c, o) -> {
                    try {
                        results.add(c.fetchReverse(new FetchKey("description")).map(s ->
                                (String) s).collect(Collectors.joining(" :: ")) + " -> " + new String(o.readAllBytes()).trim());
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
        );

        assertEquals(1, results.size());
        assertEquals("/tmp/test.rar :: nested.tgz :: hello-world.txt -> Hello, World!", results.remove());
    }
}