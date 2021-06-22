package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestContextMap {

    @Test
    void testSimpleGet() {
        ContextMap<String> contextMap = new ContextMap<>();
        Context context = Context.SEED;
        contextMap.put(context, "Hello, World!");
        assertEquals(Optional.of("Hello, World!"), contextMap.get(context));
    }

    @Test
    void testSimpleRequire() {
        ContextMap<String> contextMap = new ContextMap<>();
        Context context = Context.SEED;
        contextMap.put(context, "Hello, World!");
        assertEquals("Hello, World!", contextMap.require(context));
    }

    @Test
    void testParentGet() {
        ContextMap<String> contextMap = new ContextMap<>();
        Context context = Context.newBuilder().build();
        contextMap.put(context, "Hello, World!");
        assertEquals(Optional.of("Hello, World!"), contextMap.get(context.extend(null).build()));
    }

    @Test
    void testParentRequire() {
        ContextMap<String> contextMap = new ContextMap<>();
        Context context = Context.newBuilder().build();
        contextMap.put(context, "Hello, World!");
        assertEquals("Hello, World!", contextMap.require(context.extend(null).build()));
    }
}
