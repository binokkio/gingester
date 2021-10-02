package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.configuration.TransformerConfiguration;
import b.nana.technology.gingester.core.transformers.Monkey;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExceptionHandlingTest {

    @Test
    void test() {

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-e ExceptionHandler " +
                "-t Generate \"{string:'Hello, World!',count:2}\" " +
                "-t Monkey");

        ArrayDeque<String> results = new ArrayDeque<>();
        ArrayDeque<Exception> exceptions = new ArrayDeque<>();

        TransformerConfiguration resultHandler = new TransformerConfiguration();
        resultHandler.transformer(results::add);
        resultHandler.links(Collections.emptyList());
        gingester.add(resultHandler);

        gingester.add("ExceptionHandler", exceptions::add);

        gingester.run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!", results.getFirst());

        assertEquals(1, exceptions.size());
        assertEquals(Monkey.Bananas.class, exceptions.getFirst().getClass());
    }
}
