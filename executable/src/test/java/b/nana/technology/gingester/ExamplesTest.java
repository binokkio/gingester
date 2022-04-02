package b.nana.technology.gingester;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

class ExamplesTest {

    @Test
    void testAllExampleChecksAreOkay() {

        List<TransformerFactory.CheckExampleException> exceptions = TransformerFactory.getTransformers()
                .flatMap(transformer -> Arrays.stream(transformer.getAnnotationsByType(Example.class))
                        .map(example -> TransformerFactory.checkExample(transformer, example))
                        .flatMap(Optional::stream))
                .collect(Collectors.toList());

        if (!exceptions.isEmpty()) {

            for (TransformerFactory.CheckExampleException exception : exceptions) {
                exception.printStackTrace();
            }

            fail("Not all example checks pass");
        }
    }
}
