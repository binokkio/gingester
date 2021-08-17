package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStashFetch {

    @Test
    void test() {

        Stash<String> stash = new Stash<>();
        Fetch<String> fetch = new Fetch<>();

        AtomicReference<Object> result = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(stash, "Hello, World!");
        gBuilder.link(stash, fetch);
        gBuilder.link(fetch, result::set);
        gBuilder.build().run();

        assertEquals("Hello, World!", result.get());
    }
}
