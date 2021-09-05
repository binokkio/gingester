package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.controller.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformerFactoryTest {

    @Test
    void testAmbiguousShortName() {

        Configuration configuration = new Configuration().transformer("e");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> TransformerFactory.instance(configuration));

        assertEquals("Multiple transformers named e: Emphasize, Generate", e.getMessage());
    }

    @Test
    void testAmbiguousLongName() {

        Configuration configuration = new Configuration().transformer("Seed");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> TransformerFactory.instance(configuration));

        assertEquals("Multiple transformers named Seed: core.transformers.Seed, test.transformers.Seed", e.getMessage());
    }
}