package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExceptionHandlingTest {

    @Test
    void test() {

        AtomicReference<Exception> result = new AtomicReference<>();

        Gingester gingester = new Gingester();
        gingester.cli("-e Catch -t String.Create hello -t Time.FromMillis -- ");
        gingester.add("Catch", result::set);
        gingester.run();

        assertEquals(ClassCastException.class, result.get().getClass());
    }
}
