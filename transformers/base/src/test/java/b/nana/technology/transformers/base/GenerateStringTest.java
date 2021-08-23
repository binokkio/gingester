package b.nana.technology.transformers.base;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.receiver.SimpleReceiver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerateStringTest {

    @Test
    void testGenerateStringAppend() {

        Gingester gingester = new Gingester();

        GenerateString.Parameters parameters = new GenerateString.Parameters();
        parameters.string = "Hello, World!";
        parameters.count = 3;

//        gingester.add(new GenerateString(parameters))
//                .link(new StringAppend())
//                .link(new StringAppend());
//
        List<String> result = new ArrayList<>();
//
//        generateString.transform(null, null, (SimpleReceiver<String>) result::add);

//        assertEquals(3, result.size());
//        assertEquals("Hello, World!", result.get(0));
//        assertEquals("Hello, World!", result.get(1));
//        assertEquals("Hello, World!", result.get(2));
    }

    @Test
    void testGenerateString() {

        GenerateString.Parameters parameters = new GenerateString.Parameters();
        parameters.string = "Hello, World!";
        parameters.count = 3;

        GenerateString generateString = new GenerateString(parameters);

        List<String> result = new ArrayList<>();

        generateString.transform(null, null, (SimpleReceiver<String>) result::add);

        assertEquals(3, result.size());
        assertEquals("Hello, World!", result.get(0));
        assertEquals("Hello, World!", result.get(1));
        assertEquals("Hello, World!", result.get(2));
    }
}