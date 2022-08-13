package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FilterTest {

    @Test
    void testFilterIn() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Filter 'in.length() > 3'")
                .attach(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testFilterOut() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Filter 'in.length() < 3'")
                .attach(result::set)
                .run();

        assertNull(result.get());
    }

    @Test
    void testFilterFail() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e ExceptionHandler -t ExceptionHandler:Passthrough -- " +
                "-t StringDef 'Hello, World!' " +
                "-t Filter 'in'")
                .attach(result::set, "ExceptionHandler")
                .run();

        assertEquals("Filter did not return a boolean but returned \"Hello, World!\"", result.get().getMessage());
    }
}
