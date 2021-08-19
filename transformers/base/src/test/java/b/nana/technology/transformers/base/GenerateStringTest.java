package b.nana.technology.transformers.base;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerateStringTest {

    @Test
    void testGenerateString() {

        GenerateString.Parameters parameters = new GenerateString.Parameters();
        parameters.string = "Hello, World!";
        parameters.count = 3;

        GenerateString generateString = new GenerateString(parameters);

        List<String> result = new ArrayList<>();

        generateString.transform(null, null, (context, output) -> result.add(output));

        assertEquals(3, result.size());
        assertEquals("Hello, World!", result.get(0));
        assertEquals("Hello, World!", result.get(1));
        assertEquals("Hello, World!", result.get(2));
    }
}