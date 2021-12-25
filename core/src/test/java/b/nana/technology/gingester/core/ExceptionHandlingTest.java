package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.transformers.Monkey;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExceptionHandlingTest {

    @Test
    void test() {

        Gingester gingester = new Gingester("" +
                "-e ExceptionHandler " +
                "-t Generate \"{string:'Hello, World!',count:2}\" " +
                "-t Monkey -- " +
                "-t ExceptionHandler:Passthrough");

        ArrayDeque<String> results = new ArrayDeque<>();
        ArrayDeque<Exception> exceptions = new ArrayDeque<>();

        gingester.attach(results::add, "Monkey");
        gingester.attach(exceptions::add, "ExceptionHandler");

        gingester.run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!", results.getFirst());

        assertEquals(1, exceptions.size());
        assertEquals(Monkey.Bananas.class, exceptions.getFirst().getClass());
    }

    @Test
    void testExceptionInExceptionHandler() {

        Gingester gingester = new Gingester("" +
                "-e ExceptionHandler " +
                "-t Monkey 1 -- " +
                "-t ExceptionHandler:Monkey 1 -- " +
                "-e ResultHandler " +
                "-t ResultHandler:Passthrough");

        AtomicReference<Context> result = new AtomicReference<>();
        gingester.attach((c, e) -> result.set(c));

        gingester.run();

        assertEquals(
                "Monkey, ExceptionHandler",
                result.get().fetchReverse("transformer").map(o -> (String) o).collect(Collectors.joining(", "))
        );
    }
}
