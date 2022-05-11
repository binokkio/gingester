package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroovyTest {

    @Test
    void testYield() {

        AtomicReference<Object> result = new AtomicReference<>();

        new Gingester("-t Groovy 'yield(3 * 3)'")
                .attach(result::set)
                .run();

        assertEquals(9, result.get());
    }

    @Test
    void testOutAccept() {

        AtomicReference<Object> result = new AtomicReference<>();

        new Gingester("-t Groovy 'out.accept(context, 3 * 3)'")
                .attach(result::set)
                .run();

        assertEquals(9, result.get());
    }
}
