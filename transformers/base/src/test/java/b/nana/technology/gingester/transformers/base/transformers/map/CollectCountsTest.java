package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class CollectCountsTest {

    @Test
    void testCollectCounts() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t A:Repeat 3 " +
                "-t Repeat 3 " +
                "-t Repeat 3 " +
                "-f description " +
                "-s " +
                "-f A.description " +
                "-t MapCollectCounts " +
                "-t ObjectToJson '{enable:\"ORDER_MAP_ENTRIES_BY_KEYS\"}' " +
                "-t ResultB:ObjectToString")
                .add(result::set)
                .run();

        assertEquals("{\"0\":9,\"1\":9,\"2\":9}", result.get());
    }
}
