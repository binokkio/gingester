package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestSplit {

    @Test
    void testSplitter() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("Hello, World! Bye, World!".getBytes());
        Split.Splitter splitter = new Split.Splitter(inputStream, ", ".getBytes());
        assertEquals("Hello", new String(splitter.getNextInputStream().orElseThrow().readAllBytes()));
        assertEquals("World! Bye", new String(splitter.getNextInputStream().orElseThrow().readAllBytes()));
        assertEquals("World!", new String(splitter.getNextInputStream().orElseThrow().readAllBytes()));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplit() {

        InputStream inputStream = new ByteArrayInputStream("Hello, World! Bye, World!".getBytes());

        Split split = new Split(new Split.Parameters(", "));
        ToString toString = new ToString();

        List<String> results = new ArrayList<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(split, inputStream);
        gBuilder.link(split, toString);
        gBuilder.link(toString, (Consumer<String>) results::add);
        gBuilder.build().run();

        assertEquals("Hello", results.get(0));
        assertEquals("World! Bye", results.get(1));
        assertEquals("World!", results.get(2));
        assertEquals(results.size(), 3);
    }
}
