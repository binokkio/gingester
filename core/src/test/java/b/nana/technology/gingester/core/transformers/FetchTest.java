package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FetchTest {

    @Test
    void testSimple() throws Exception {

        AtomicReference<Object> result = new AtomicReference<>();

        Context context = new Context.Builder()
                .stash("hello", "Hello, World!")
                .build();

        Fetch fetch = new Fetch(new Fetch.Parameters("hello"));
        fetch.transform(context, null, (UniReceiver<Object>) result::set);

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testFetchNameParsing() throws Exception {

        AtomicReference<Object> result = new AtomicReference<>();

        Context context = new Context.Builder()
                .stash(
                        "hello", Map.of(
                                "world", "Hello, World!"
                        )
                )
                .build();

        Fetch fetch = new Fetch(new Fetch.Parameters("hello.world"));
        fetch.transform(context, null, (UniReceiver<Object>) result::set);

        assertEquals("Hello, World!", result.get());
    }
}