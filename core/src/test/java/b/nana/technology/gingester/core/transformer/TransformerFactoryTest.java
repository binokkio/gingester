package b.nana.technology.gingester.core.transformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformerFactoryTest {

    @Test
    void testAmbiguousName() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> TransformerFactory.instance("NameCollision"));

        assertEquals("Multiple transformers named NameCollision: ANameCollision, BNameCollision", e.getMessage());
    }
}