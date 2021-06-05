package b.nana.technology.gingester.core;

import b.nana.technology.gingester.test.transformers.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TestGingester {

    @Test
    void testEmphasizeLinkFirst() {
        AtomicReference<String> result = new AtomicReference<>();
        Emphasize emphasize = new Emphasize();
        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.link(emphasize, result::set);
        gBuilder.seed(emphasize, "Hello, World");
        gBuilder.build().run();
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testEmphasizeSeedFirst() {
        AtomicReference<String> result = new AtomicReference<>();
        Emphasize emphasize = new Emphasize();
        Gingester.Builder gBuilder = Gingester.newBuilder();
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
        Gingester.Builder gBuilder = Gingester.newBuilder();
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
        Gingester.Builder gBuilder = Gingester.newBuilder();
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
        Gingester.Builder gBuilder = Gingester.newBuilder();
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
        assertThrows(IllegalStateException.class, () -> new Builder().sync(new Emphasize(), new Emphasize()));
    }

    @Test
    void testTransformCalledByUpstreamWorkerByDefault() {
        Set<String> names = Collections.synchronizedSet(new HashSet<>());
        Gingester.Builder gBuilder = Gingester.newBuilder();
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
        Gingester.Builder gBuilder = Gingester.newBuilder();
        YieldThreadName yieldThreadName = new YieldThreadName(false);
        gBuilder.link(yieldThreadName, names::add);
        gBuilder.link(new Generate("Hello!"), yieldThreadName).sync();
        gBuilder.link(new Generate("Hello!"), yieldThreadName).sync();
        gBuilder.link(new Generate("Hello!"), yieldThreadName).sync();
        gBuilder.build().run();
        assertEquals(3, names.size());
    }

    @Test
    void testTransformCalledByDifferentWorkerWhenLinkIsAsync() {
        Set<String> names = Collections.synchronizedSet(new HashSet<>());
        Gingester.Builder gBuilder = Gingester.newBuilder();
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
        Gingester.Builder gBuilder = Gingester.newBuilder();
        Emphasize emphasize = new Emphasize();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> gBuilder.link(emphasize, emphasize));
        assertEquals("Linking from Emphasize to Emphasize would create a circular route", e.getMessage());
    }

    @Test
    void testCircularLinkingIsIllegal() {
        Gingester.Builder gBuilder = Gingester.newBuilder();
        Emphasize emphasize1 = new Emphasize();
        Emphasize emphasize2 = new Emphasize();
        gBuilder.name("Emphasize-1", emphasize1);
        gBuilder.name("Emphasize-2", emphasize2);
        gBuilder.link(emphasize1, emphasize2);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> gBuilder.link(emphasize2, emphasize1));
        assertEquals("Linking from Emphasize-2 to Emphasize-1 would create a circular route", e.getMessage());
    }

    @Test
    void testCircularLinkThroughExceptionHandlerIsIllegal() {
        Gingester.Builder gBuilder = Gingester.newBuilder();
        Emphasize emphasize = new Emphasize();
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        gBuilder.link(exceptionHandler, emphasize);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> gBuilder.except(emphasize, exceptionHandler));
        assertEquals("Linking from Emphasize to ExceptionHandler would create a circular route", e.getMessage());
    }

    @Test
    void testExceptionHandling() {

        Generate generate = new Generate("Hello, World");
        ExceptionThrower exceptionThrower = new ExceptionThrower();
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        Emphasize emphasize = new Emphasize();

        AtomicReference<String> exceptionHandlerResult = new AtomicReference<>();
        AtomicReference<String> emphasizeResult = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.link(generate, exceptionThrower);
        gBuilder.link(exceptionThrower, emphasize);
        gBuilder.except(exceptionThrower, exceptionHandler);
        gBuilder.link(emphasize, emphasizeResult::set);
        gBuilder.link(exceptionHandler, exceptionHandlerResult::set);
        gBuilder.build().run();

        assertEquals("ExceptionThrower throws", exceptionHandlerResult.get());
        assertNull(emphasizeResult.get());
    }

    @Test
    void testExceptionHandlerCanHaveNormalLink() {

        Generate generate = new Generate("Hello, World");
        ExceptionThrower exceptionThrower = new ExceptionThrower();
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        Emphasize emphasize = new Emphasize();

        AtomicReference<String> emphasizeResult = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.link(generate, exceptionThrower);
        gBuilder.link(exceptionThrower, emphasize);
        gBuilder.except(exceptionThrower, exceptionHandler);
        gBuilder.link(emphasize, emphasizeResult::set);
        gBuilder.link(exceptionHandler, emphasize);
        gBuilder.link(emphasize, emphasizeResult::set);
        gBuilder.build().run();

        assertEquals("ExceptionThrower throws!", emphasizeResult.get());
    }
}
