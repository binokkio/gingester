package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.context.Context;
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
    void testUnpack() throws IOException {

        Context context = new Context.Builder()
                .stash(Map.of("description", "/tmp/test.tar.gz"))
                .build();

        Queue<String> results = new ArrayDeque<>();

        Unpack unpack = new Unpack();
        unpack.transform(
                context,
                getClass().getResourceAsStream("/test.tar.gz"),
                (BiReceiver<InputStream>) (c, o) -> {
                    try {
                        results.add(c.fetchReverse("description").map(s ->
                                (String) s).collect(Collectors.joining(" :: ")) + " -> " + new String(o.readAllBytes()).trim());
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
        );

        results.forEach(System.out::println);

        assertEquals(4, results.size());
        assertEquals("/tmp/test.tar.gz :: test.tar :: b.gz :: b -> Hello, World!", results.remove());
        assertEquals("/tmp/test.tar.gz :: test.tar :: c -> Hello, World!", results.remove());
        assertEquals("/tmp/test.tar.gz :: test.tar :: dir/d.bz2 :: d -> Hello, World", results.remove());
        assertEquals("/tmp/test.tar.gz :: test.tar :: test.zip :: a -> Hello, World!", results.remove());
    }
}