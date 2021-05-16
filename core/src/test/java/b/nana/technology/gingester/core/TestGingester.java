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
        Gingester gingester = new Gingester();
        gingester.link(emphasize, result::set);
        gingester.seed(emphasize, "Hello, World");
        gingester.run();
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testEmphasizeSeedFirst() {
        AtomicReference<String> result = new AtomicReference<>();
        Emphasize emphasize = new Emphasize();
        Gingester gingester = new Gingester();
        gingester.seed(emphasize, "Hello, World");
        gingester.link(emphasize, result::set);
        gingester.run();
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testEmphasizeTwice() {
        AtomicReference<String> result = new AtomicReference<>();
        Emphasize[] emphasizers = new Emphasize[] {
                new Emphasize(), new Emphasize()
        };
        Gingester gingester = new Gingester();
        gingester.link(emphasizers[0], emphasizers[1]);
        gingester.link(emphasizers[1], result::set);
        gingester.seed(emphasizers[0], "Hello, World");
        gingester.run();
        assertEquals("Hello, World!!", result.get());
    }

    @Test
    void testEmphasizeThrice() {
        AtomicReference<String> result = new AtomicReference<>();
        Emphasize[] emphasizers = new Emphasize[] {
                new Emphasize(), new Emphasize(), new Emphasize()
        };
        Gingester gingester = new Gingester();
        gingester.link(emphasizers[0], emphasizers[1]);
        gingester.link(emphasizers[1], emphasizers[2]);
        gingester.link(emphasizers[2], result::set);
        gingester.seed(emphasizers[0], "Hello, World");
        gingester.run();
        assertEquals("Hello, World!!!", result.get());
    }

    @Test
    void testEmphasizeOnceTwiceThrice() {
        Set<String> results = Collections.synchronizedSet(new HashSet<>());
        Emphasize[] emphasizers = new Emphasize[] {
                new Emphasize(), new Emphasize(), new Emphasize()
        };
        Gingester gingester = new Gingester();
        gingester.link(emphasizers[0], emphasizers[1]);
        gingester.link(emphasizers[1], emphasizers[2]);
        gingester.link(emphasizers[2], results::add);
        gingester.seed(emphasizers[0], "Hello, World");
        gingester.seed(emphasizers[1], "Hello, World");
        gingester.seed(emphasizers[2], "Hello, World");
        gingester.run();
        assertEquals(Set.of("Hello, World!", "Hello, World!!", "Hello, World!!!"), results);
    }

    @Test
    void testSyncBeforeLinkThrows() {
        assertThrows(IllegalStateException.class, () -> new Gingester().sync(new Emphasize(), new Emphasize()));
    }

    @Test
    void testTransformCalledByDedicatedWorkerByDefault() {
        Set<String> names = Collections.synchronizedSet(new HashSet<>());
        Gingester gingester = new Gingester();
        YieldThreadName yieldThreadName = new YieldThreadName(false);
        gingester.link(yieldThreadName, names::add);
        gingester.link(new Generate("Hello!"), yieldThreadName);
        gingester.link(new Generate("Hello!"), yieldThreadName);
        gingester.link(new Generate("Hello!"), yieldThreadName);
        gingester.run();
        assertEquals(1, names.size());
    }

    @Test
    void testTransformCalledByDownstreamWorkerWhenLinkIsSynced() {
        Set<String> names = Collections.synchronizedSet(new HashSet<>());
        Gingester gingester = new Gingester();
        YieldThreadName yieldThreadName = new YieldThreadName(false);
        gingester.link(yieldThreadName, names::add);
        gingester.link(new Generate("Hello!"), yieldThreadName).sync();
        gingester.link(new Generate("Hello!"), yieldThreadName).sync();
        gingester.link(new Generate("Hello!"), yieldThreadName).sync();
        gingester.run();
        assertEquals(3, names.size());
    }
}
