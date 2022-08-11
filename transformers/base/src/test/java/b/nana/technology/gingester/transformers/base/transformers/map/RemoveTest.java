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
                "-t StringDef 'Hello, World!' " +
                "-s " +
                "-l A B C " +
                "-t A:StringDef A -l MapCollect " +
                "-t B:StringDef B -l MapCollect " +
                "-t C:StringDef C -l MapCollect " +
                "-t MapCollect " +
                "-s " +
                "-t StringDef B " +
                "-t MapRemove " +
                "-f")
                .attach(result::set)
                .attach(mapRemoveResult::set, "MapRemove")
                .run();

        assertEquals("Hello, World!", result.get().get("A"));
        assertEquals("Hello, World!", mapRemoveResult.get());
        assertEquals("Hello, World!", result.get().get("C"));
        assertEquals(2, result.get().size());
    }

    @Test
    void testEmptyRemove() {

        AtomicReference<String> mapRemoveResult = new AtomicReference<>();
        AtomicReference<Map<?, ?>> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-s " +
                "-l A B C " +
                "-t A:StringDef A -l MapCollect " +
                "-t B:StringDef B -l MapCollect " +
                "-t C:StringDef C -l MapCollect " +
                "-t MapCollect " +
                "-s " +
                "-t StringDef D " +
                "-t MapRemove " +
                "-f")
                .attach(result::set)
                .attach(mapRemoveResult::set, "MapRemove")
                .run();

        assertEquals("Hello, World!", result.get().get("A"));
        assertEquals("Hello, World!", result.get().get("B"));
        assertEquals("Hello, World!", result.get().get("C"));
        assertEquals(3, result.get().size());
        assertNull(mapRemoveResult.get());
    }
}