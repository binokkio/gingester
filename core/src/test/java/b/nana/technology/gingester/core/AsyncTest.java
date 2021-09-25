package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncTest {

    @Test
    void test() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("-sft Generate \"Hello, World!\"");
        gingester.cli("-stt Sync");
        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from Sync finish()", result.get());
    }
}
