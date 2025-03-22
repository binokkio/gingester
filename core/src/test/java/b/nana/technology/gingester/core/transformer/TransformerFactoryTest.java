package b.nana.technology.gingester.core.transformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformerFactoryTest {

    private final TransformerFactory transformerFactory = new TransformerFactory();

    @Test
    void testUnknownName() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> transformerFactory.instance("Unknown"));

        assertEquals("No transformer named Unknown", e.getMessage());
    }

    @Test
    void testAmbiguousName() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> transformerFactory.instance("NameCollision"));

        assertEquals("Multiple transformers named NameCollision: ANameCollision, BNameCollision", e.getMessage());
    }

    @Test
    void testPartialNameOneOption() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> transformerFactory.instance("Foo"));

        assertEquals("No transformer named Foo, maybe TestFoo?", e.getMessage());
    }

    @Test
    void testPartialNameMultipleOptions() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> transformerFactory.instance("Collision"));

        assertEquals("No transformer named Collision, maybe one of ANameCollision, BNameCollision?", e.getMessage());
    }
}