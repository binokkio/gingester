package b.nana.technology.gingester.core;

import b.nana.technology.gingester.test.transformers.Emphasize;
import b.nana.technology.gingester.test.transformers.Generate;
import b.nana.technology.gingester.test.transformers.YieldThreadName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestGingester {

    @Test
    void testEmphasizeLinkFirst() {
        AtomicReference<String> result = new AtomicReference<>();
        Emphasize emphasize = new Emphasize();
        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.link(emphasize, result::set);
        gBuilder.seed(emphasize, "Hello, World");
        gBuilder.build().run();
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testEmphasizeSeedFirst() {
        AtomicReference<String> result = new AtomicReference<>();
        Emphasize emphasize = new Emphasize();
        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.seed(emphasize, "Hello, World");
        gBuilder.link(emphasize, result::set);
        gBuilder.build().run();
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testEmphasizeTwice() {
        AtomicReference<String> result = new AtomicReference<>();
        Emphasize[] emphasizers = new Emphasize[] {
                new Emphasize(), new Emphasize()
        };
        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.link(emphasizers[0], emphasizers[1]);
        gBuilder.link(emphasizers[1], result::set);
        gBuilder.seed(emphasizers[0], "Hello, World");
        gBuilder.build().run();
        assertEquals("Hello, World!!", result.get());
    }

    @Test
    void testEmphasizeThrice() {
        AtomicReference<String> result = new AtomicReference<>();
        Emphasize[] emphasizers = new Emphasize[] {
                new Emphasize(), new Emphasize(), new Emphasize()
        };
        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.link(emphasizers[0], emphasizers[1]);
        gBuilder.link(emphasizers[1], emphasizers[2]);
        gBuilder.link(emphasizers[2], result::set);
        gBuilder.seed(emphasizers[0], "Hello, World");
        gBuilder.build().run();
        assertEquals("Hello, World!!!", result.get());
    }

    @Test
    void testEmphasizeOnceTwiceThrice() {
        Set<String> results = Collections.synchronizedSet(new HashSet<>());
        Emphasize[] emphasizers = new Emphasize[] {
                new Emphasize(), new Emphasize(), new Emphasize()
        };
        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.link(emphasizers[0], emphasizers[1]);
        gBuilder.link(emphasizers[1], emphasizers[2]);
        gBuilder.link(emphasizers[2], results::add);
        gBuilder.seed(emphasizers[0], "Hello, World");
        gBuilder.seed(emphasizers[1], "Hello, World");
        gBuilder.seed(emphasizers[2], "Hello, World");
        gBuilder.build().run();
        assertEquals(Set.of("Hello, World!", "Hello, World!!", "Hello, World!!!"), results);
    }

    @Test
    void testSyncBeforeLinkThrows() {
        assertThrows(IllegalStateException.class, () -> new Gingester.Builder().sync(new Emphasize(), new Emphasize()));
    }

    @Test
    void testTransformCalledByUpstreamWorkerByDefault() {
        Set<String> names = Collections.synchronizedSet(new HashSet<>());
        Gingester.Builder gBuilder = new Gingester.Builder();
        YieldThreadName yieldThreadName = new YieldThreadName(false);
        gBuilder.link(yieldThreadName, names::add);
        gBuilder.link(new Generate("Hello!"), yieldThreadName);
        gBuilder.link(new Generate("Hello!"), yieldThreadName);
        gBuilder.link(new Generate("Hello!"), yieldThreadName);
        gBuilder.build().run();
        assertEquals(3, names.size());
    }

    @Test
    void testTransformCalledByUpstreamWorkerWhenLinkIsSync() {
        Set<String> names = Collections.synchronizedSet(new HashSet<>());
        Gingester.Builder gBuilder = new Gingester.Builder();
        YieldThreadName yieldThreadName = new YieldThreadName(false);
        gBuilder.link(yieldThreadName, names::add);
        gBuilder.link(new Generate("Hello!"), yieldThreadName).sync();
        gBuilder.link(new Generate("Hello!"), yieldThreadName).sync();
        gBuilder.link(new Generate("Hello!"), yieldThreadName).sync();
        gBuilder.build().run();
        assertEquals(3, names.size());
    }

    @Test
    void testTransformCalledByDownstreamWorkerWhenLinkIsAsync() {
        Set<String> names = Collections.synchronizedSet(new HashSet<>());
        Gingester.Builder gBuilder = new Gingester.Builder();
        YieldThreadName yieldThreadName = new YieldThreadName(false);
        gBuilder.link(yieldThreadName, names::add);
        gBuilder.link(new Generate("Hello!"), yieldThreadName).async();
        gBuilder.link(new Generate("Hello!"), yieldThreadName).async();
        gBuilder.link(new Generate("Hello!"), yieldThreadName).async();
        gBuilder.build().run();
        assertEquals(1, names.size());
    }

    @Test
    void testSelfLinkingIsIllegal() {
        Gingester.Builder gBuilder = new Gingester.Builder();
        Emphasize emphasize = new Emphasize();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> gBuilder.link(emphasize, emphasize));
        assertEquals("Linking from Emphasize to Emphasize would create a circular route", e.getMessage());
    }

    @Test
    void testCircularLinkingIsIllegal() {
        Gingester.Builder gBuilder = new Gingester.Builder();
        Emphasize emphasize1 = new Emphasize();
        Emphasize emphasize2 = new Emphasize();
        gBuilder.name("Emphasize-1", emphasize1);
        gBuilder.name("Emphasize-2", emphasize2);
        gBuilder.link(emphasize1, emphasize2);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> gBuilder.link(emphasize2, emphasize1));
        assertEquals("Linking from Emphasize-2 to Emphasize-1 would create a circular route", e.getMessage());
    }
}
