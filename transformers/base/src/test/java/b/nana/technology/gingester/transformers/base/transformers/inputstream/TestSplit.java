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
    void testSplitterReadAllBytes() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("Hello, World! Bye, World!".getBytes());
        Split.Splitter splitter = new Split.Splitter(inputStream, ", ".getBytes());
        assertEquals("Hello", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("World! Bye", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("World!", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterReadSingleBytes() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("Hello, World! Bye, World!".getBytes());
        Split.Splitter splitter = new Split.Splitter(inputStream, ", ".getBytes());
        assertEquals("Hello", readSingleBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("World! Bye", readSingleBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("World!", readSingleBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitWithPartialDelimiterAtEndOfStream() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("Hello, World!DELIMITERBye, World!DELIM".getBytes());
        Split.Splitter splitter = new Split.Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("Hello, World!", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("Bye, World!DELIM", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
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

    private String readAllBytesToString(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes());
    }

    private String readSingleBytesToString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int read;
        while ((read = inputStream.read()) != -1) {
            stringBuilder.append((char) read);
        }
        return stringBuilder.toString();
    }
}
