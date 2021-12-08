package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircularRouteDetectionTest {

    @Test
    void test() {
        Gingester gingester = new Gingester();
        gingester.cli("-t Stash -l Stash");
        IllegalStateException e = assertThrows(IllegalStateException.class, gingester::run);
        assertEquals("Circular route detected", e.getMessage());
    }
}
