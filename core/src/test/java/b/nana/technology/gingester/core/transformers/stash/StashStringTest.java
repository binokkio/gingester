package b.nana.technology.gingester.core.transformers.stash;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.controller.Context;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

class StashStringTest {

    @Test
    void test() {

        AtomicReference<String> hello = new AtomicReference<>();
        AtomicReference<String> bye = new AtomicReference<>();

        new Gingester().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-ss bye 'Bye, World!'")
                .attach(((BiConsumer<Context, String>) (context, value) -> {
                    hello.set(value);
                    bye.set((String) context.require("bye"));
                }))
                .run();

        assertEquals("Hello, World!", hello.get());
        assertEquals("Bye, World!", bye.get());
    }
}