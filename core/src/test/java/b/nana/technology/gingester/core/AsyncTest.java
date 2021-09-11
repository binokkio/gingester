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

        gingester.configure(c -> c
                .transformer("Generate")
                .parameters("Hello, World!"));

        gingester.configure(c -> c
                .transformer("Sync")
                .syncs(List.of("Generate")));

        gingester.configure(c -> c
                .transformer(result::set)
                .maxWorkers(1));

        gingester.run();

        assertEquals("Message from Sync finish()", result.get());
    }
}
