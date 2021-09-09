package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.controller.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformerFactoryTest {

    @Test
    void testAmbiguousName() {

        Configuration configuration = new Configuration().transformer("Seed");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> TransformerFactory.instance(configuration));

        assertEquals("Multiple transformers named Seed: core.transformers.Seed, test.transformers.Seed", e.getMessage());
    }
}