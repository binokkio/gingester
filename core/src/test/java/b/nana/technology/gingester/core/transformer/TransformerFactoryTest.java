package b.nana.technology.gingester.core.transformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformerFactoryTest {

    @Test
    void testUnknownName() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> TransformerFactory.instance("Unknown"));

        assertEquals("No transformer named Unknown", e.getMessage());
    }

    @Test
    void testAmbiguousName() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> TransformerFactory.instance("NameCollision"));

        assertEquals("Multiple transformers named NameCollision: ANameCollision, BNameCollision", e.getMessage());
    }

    @Test
    void testPartialNameOneOption() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> TransformerFactory.instance("Foo"));

        assertEquals("No transformer named Foo, maybe TestFoo?", e.getMessage());
    }

    @Test
    void testPartialNameMultipleOptions() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> TransformerFactory.instance("Collision"));

        assertEquals("No transformer named Collision, maybe one of ANameCollision, BNameCollision?", e.getMessage());
    }
}