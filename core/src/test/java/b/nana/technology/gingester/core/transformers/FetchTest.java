package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FetchTest {

    @Test
    void testSimple() throws Exception {

        AtomicReference<Object> result = new AtomicReference<>();

        Context context = Context.newTestContext()
                .stash("hello", "Hello, World!")
                .buildForTesting();

        Fetch fetch = new Fetch(new Fetch.Parameters("hello"));
        fetch.transform(context, null, (UniReceiver<Object>) result::set);

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testFetchNameParsing() throws Exception {

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

        new Gingester().cli("" +
                "-t Generate hello " +
                "-s " +
                "-t Generate world " +
                "-s " +
                "-f ^1 " +
                "-f ^2")
                .attach(resultUp1::set, "Fetch")
                .attach(resultUp2::set, "Fetch_1")
                .run();

        assertEquals("world", resultUp1.get());
        assertEquals("hello", resultUp2.get());
    }
}