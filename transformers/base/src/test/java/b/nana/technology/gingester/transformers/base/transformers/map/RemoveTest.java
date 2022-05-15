package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RemoveTest {

    @Test
    void test() {

        AtomicReference<String> mapRemoveResult = new AtomicReference<>();
        AtomicReference<Map<?, ?>> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t StringCreate 'Hello, World!' " +
                "-s " +
                "-l A B C " +
                "-t A:StringCreate A -l MapCollect " +
                "-t B:StringCreate B -l MapCollect " +
                "-t C:StringCreate C -l MapCollect " +
                "-t MapCollect " +
                "-s " +
                "-t StringCreate B " +
                "-t MapRemove " +
                "-f")
                .attach(mapRemoveResult::set, "MapRemove")
                .attach(result::set)
                .run();

        assertEquals("Hello, World!", result.get().get("A"));
        assertEquals("Hello, World!", mapRemoveResult.get());
        assertEquals("Hello, World!", result.get().get("C"));
        assertEquals(2, result.get().size());
    }

}