package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import b.nana.technology.gingester.core.transformers.stash.Fetch;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FetchTest {

    @Test
    void testSimple() {

        AtomicReference<Object> result = new AtomicReference<>();

        Context context = Context.newTestContext()
                .stash("hello", "Hello, World!")
                .buildForTesting();

        Fetch fetch = new Fetch(new Fetch.Parameters("hello"));
        fetch.transform(context, null, (UniReceiver<Object>) result::set);

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testFetchNameParsing() {

        AtomicReference<Object> result = new AtomicReference<>();

        Context context = Context.newTestContext()
                .stash(
                        "hello", Map.of(
                                "world", "Hello, World!"
                        )
                )
                .buildForTesting();

        Fetch fetch = new Fetch(new Fetch.Parameters("hello.world"));
        fetch.transform(context, null, (UniReceiver<Object>) result::set);

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testFetchOrdinal() {

        AtomicReference<String> resultUp1 = new AtomicReference<>();
        AtomicReference<String> resultUp2 = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef hello " +
                "-s " +
                "-t StringDef world " +
                "-s " +
                "-f ^1 " +
                "-f ^2")
                .addTo(resultUp1::set, "Fetch")
                .addTo(resultUp2::set, "Fetch_1")
                .run();

        assertEquals("world", resultUp1.get());
        assertEquals("hello", resultUp2.get());
    }

    @Test
    void testFetchOptional() {

        AtomicReference<String> resultHit = new AtomicReference<>();
        AtomicReference<String> resultMiss = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef hello " +
                "-s hit " +
                "-l Hit Miss " +
                "-t Hit:Fetch hit optional " +
                "-- " +
                "-t Miss:Fetch miss optional")
                .addTo(resultHit::set, "Hit")
                .addTo(resultMiss::set, "Miss")
                .run();

        assertEquals("hello", resultHit.get());
        assertNull(resultMiss.get());
    }
}