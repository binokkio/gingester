package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.receiver.UniReceiver;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TrimTest {

    @Test
    void testTrim() {
        AtomicReference<String> result = new AtomicReference<>();
        new Trim(new Trim.Parameters()).transform(null, " Hello, World! ", (UniReceiver<String>) result::set);
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testTrimDoNotDropEmpty() {
        AtomicReference<String> result = new AtomicReference<>();
        new Trim(new Trim.Parameters()).transform(null, "  ", (UniReceiver<String>) result::set);
        assertEquals("", result.get());
    }

    @Test
    void testTrimDropEmpty() {
        AtomicReference<String> result = new AtomicReference<>();
        new Trim(new Trim.Parameters(true)).transform(null, "  ", (UniReceiver<String>) result::set);
        assertNull(result.get());
    }
}