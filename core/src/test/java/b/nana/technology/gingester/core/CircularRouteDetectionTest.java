package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircularRouteDetectionTest {

    @Test
    void test() {
        Gingester gingester = new Gingester().cli("-t Generate 'Hello, World!' -t Stash -t Fetch -l Generate");
        IllegalStateException e = assertThrows(IllegalStateException.class, gingester::run);
        assertEquals("Circular route detected: Generate -> Stash -> Fetch -> Generate", e.getMessage());
    }

    @Test
    void testMinimal() {
        Gingester gingester = new Gingester().cli("-s -l Stash");
        IllegalStateException e = assertThrows(IllegalStateException.class, gingester::run);
        assertEquals("Circular route detected: Stash -> Stash", e.getMessage());
    }

    @Test
    void testCircularRouteThroughExceptionHandler() {
        Gingester gingester = new Gingester().cli("-e ExceptionHandler -t Generate 'Hello, World!' -- -t ExceptionHandler:Stash -l Generate");
        IllegalStateException e = assertThrows(IllegalStateException.class, gingester::run);
        assertEquals("Circular route detected: Generate -> ExceptionHandler -> Generate", e.getMessage());
    }
}
