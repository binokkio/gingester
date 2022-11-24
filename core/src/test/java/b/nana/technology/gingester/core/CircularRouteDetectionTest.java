package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircularRouteDetectionTest {

    @Test
    void test() {
        FlowBuilder flowBuilder = new FlowBuilder().cli("-t StringDef 'Hello, World!' -t Stash -t Fetch -l StringDef");
        IllegalStateException e = assertThrows(IllegalStateException.class, flowBuilder::run);
        assertEquals("Circular route detected: StringDef -> Stash -> Fetch -> StringDef", e.getMessage());
    }

    @Test
    void testMinimal() {
        FlowBuilder flowBuilder = new FlowBuilder().cli("-s -l Stash");
        IllegalStateException e = assertThrows(IllegalStateException.class, flowBuilder::run);
        assertEquals("Circular route detected: Stash -> Stash", e.getMessage());
    }

    @Test
    void testCircularRouteThroughExceptionHandler() {
        FlowBuilder flowBuilder = new FlowBuilder().cli("-e ExceptionHandler -t StringDef 'Hello, World!' -- -t ExceptionHandler:Stash -l StringDef");
        IllegalStateException e = assertThrows(IllegalStateException.class, flowBuilder::run);
        assertEquals("Circular route detected: StringDef -> ExceptionHandler -> StringDef", e.getMessage());
    }

    @Test
    void testDetachedIsland() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t StringDef 'Not part of the island.' -- " +
                "-t Island:StringDef 'Part of the island.' -l Island");

        IllegalStateException e = assertThrows(IllegalStateException.class, flowBuilder::run);
        assertEquals("Circular route detected: Island -> Island", e.getMessage());
    }
}
