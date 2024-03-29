package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CollectCollectionsTest {

    @Test
    void testCollectLists() {

        AtomicReference<String> resultA = new AtomicReference<>();
        AtomicReference<String> resultB = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t A:Repeat 3 " +
                "-t B:Repeat 3 " +
                "-t StringDef '${A.description}.${B.description}' " +
                "-s " +
                "-l OnA OnB " +
                "-t OnA:Fetch A.description " +
                "-t MapCollectLists " +
                "-t ObjectToJson '{enable:\"ORDER_MAP_ENTRIES_BY_KEYS\"}' " +
                "-t ResultA:ObjectToString " +
                "-- " +
                "-t OnB:Fetch B.description " +
                "-t BLists:MapCollectLists " +
                "-t ObjectToJson '{enable:\"ORDER_MAP_ENTRIES_BY_KEYS\"}' " +
                "-t ResultB:ObjectToString")
                .addTo(resultA::set, "ResultA")
                .addTo(resultB::set, "ResultB")
                .run();

        assertEquals("{\"0\":[\"0.0\",\"0.1\",\"0.2\"],\"1\":[\"1.0\",\"1.1\",\"1.2\"],\"2\":[\"2.0\",\"2.1\",\"2.2\"]}", resultA.get());
        assertEquals("{\"0\":[\"0.0\",\"1.0\",\"2.0\"],\"1\":[\"0.1\",\"1.1\",\"2.1\"],\"2\":[\"0.2\",\"1.2\",\"2.2\"]}", resultB.get());
    }

    @Test
    void testCollectSets() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t A:Repeat 3 " +
                "-t Repeat 3 " +
                "-t Repeat 3 " +
                "-f description " +
                "-s " +
                "-f A.description " +
                "-t MapCollectSets " +
                "-t ObjectToJson '{enable:\"ORDER_MAP_ENTRIES_BY_KEYS\"}' " +
                "-t ResultB:ObjectToString")
                .add(result::set)
                .run();

        assertEquals("{\"0\":[0,1,2],\"1\":[0,1,2],\"2\":[0,1,2]}", result.get());
    }
}