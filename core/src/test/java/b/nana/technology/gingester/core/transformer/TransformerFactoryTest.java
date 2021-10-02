package b.nana.technology.gingester.core.transformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransformerFactoryTest {

    @Test
    void testAmbiguousName() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> TransformerFactory.instance("NameCollision"));

        assertEquals("Multiple transformers named NameCollision: A.NameCollision, B.NameCollision", e.getMessage());
    }
}