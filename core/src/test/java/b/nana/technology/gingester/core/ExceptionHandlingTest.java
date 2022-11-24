package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.transformers.Monkey;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionHandlingTest {

    @Test
    void test() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-e ExceptionHandler " +
                "-t StringDef 'Hello, World!' " +
                "-t Repeat 2 " +
                "-t Monkey -- " +
                "-t ExceptionHandler:Passthrough");

        ArrayDeque<String> results = new ArrayDeque<>();
        ArrayDeque<Exception> exceptions = new ArrayDeque<>();

        flowBuilder.addTo(results::add, "Monkey");
        flowBuilder.addTo(exceptions::add, "ExceptionHandler");

        flowBuilder.run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!", results.getFirst());

        assertEquals(1, exceptions.size());
        assertEquals(Monkey.Bananas.class, exceptions.getFirst().getClass());
    }

    @Test
    void testExceptionInExceptionHandler() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-e ExceptionHandler " +
                "-t Monkey 1 -- " +
                "-t ExceptionHandler:Monkey 1 -- " +
                "-e ResultHandler " +
                "-t ResultHandler:Passthrough");

        AtomicReference<Context> result = new AtomicReference<>();
        flowBuilder.add((c, e) -> result.set(c));

        flowBuilder.run();

        assertEquals(
                "Monkey, ExceptionHandler",
                result.get().fetchReverse("transformer").map(o -> (String) o).collect(Collectors.joining(", "))
        );
    }

    @Test
    void testExceptionBubblesToSeed() {

        AtomicReference<Context> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e ExceptionHandler " +
                "-t StringDef hello " +
                "-t Monkey 1 -- " +
                "-t ExceptionHandler:Void")
                .addTo((context, object) -> result.set(context), "__seed__")
                .run();

        assertFalse(result.get().isFlawless());
    }
}
