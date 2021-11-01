package b.nana.technology.gingester.transformers.base.transformers.filter;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import b.nana.technology.gingester.transformers.base.transformers.util.Latch;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LatchTest {

    @Test
    void test() throws Exception {

        Latch latch = new Latch(new Latch.Parameters("foo"));

        Context seed = new Context.Builder().build();
        latch.prepare(seed, null);

        Context contextA = seed.stash("foo", "a").build();
        Context contextB = seed.stash("foo", "b").build();

        AtomicReference<Object> one = new AtomicReference<>();
        latch.transform(contextA, "Hello, World 1!", (UniReceiver<Object>) one::set);

        AtomicReference<Object> two = new AtomicReference<>();
        latch.transform(contextA, "Hello, World 2!", (UniReceiver<Object>) two::set);

        AtomicReference<Object> three = new AtomicReference<>();
        latch.transform(contextB, "Hello, World 3!", (UniReceiver<Object>) three::set);

        assertEquals("Hello, World 1!", one.get());
        assertNull(two.get());
        assertEquals("Hello, World 3!", three.get());
    }
}