package b.nana.technology.gingester.core.transformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransformerFactoryTest {

    @Test
    void testAmbiguousName() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> TransformerFactory.instance("Seed"));

        assertEquals("Multiple transformers named Seed: Core.Transformers.Seed, Test.Transformers.Seed", e.getMessage());
    }
}