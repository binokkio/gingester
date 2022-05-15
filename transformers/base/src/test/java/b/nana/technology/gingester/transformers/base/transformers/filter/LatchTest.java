package b.nana.technology.gingester.transformers.base.transformers.filter;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import b.nana.technology.gingester.transformers.base.transformers.util.Latch;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LatchTest {

    @Test
    void test() throws Exception {

        Context seed = Context.newTestContext();

        Latch latch = new Latch();
        latch.prepare(seed, null);

        AtomicReference<Object> one = new AtomicReference<>();
        latch.transform(seed, "Hello, World!", (UniReceiver<Object>) one::set);

        AtomicReference<Object> two = new AtomicReference<>();
        latch.transform(seed, "Hello, World!", (UniReceiver<Object>) two::set);

        AtomicReference<Object> three = new AtomicReference<>();
        latch.transform(seed, "Hello, different World!", (UniReceiver<Object>) three::set);

        assertEquals("Hello, World!", one.get());
        assertNull(two.get());
        assertEquals("Hello, different World!", three.get());
    }
}