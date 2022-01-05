package b.nana.technology.gingester.transformers.base.common.iostream;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static b.nana.technology.gingester.transformers.base.common.iostream.Helpers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplitterTest {

    @Test
    void testSplitterReadAllBytes() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("Hello, World! Bye, World!".getBytes());
        Splitter splitter = new Splitter(inputStream, ", ".getBytes());
        assertEquals("Hello", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("World! Bye", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("World!", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterReadSingleBytes() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("Hello, World! Bye, World!".getBytes());
        Splitter splitter = new Splitter(inputStream, ", ".getBytes());
        assertEquals("Hello", readSingleBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("World! Bye", readSingleBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("World!", readSingleBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterWithPartialDelimiterAtEndOfStream() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("Hello, World!DELIMITERBye, World!DELIM".getBytes());
        Splitter splitter = new Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("Hello, World!", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("Bye, World!DELIM", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterWithAdjacentDelimiters() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("HelloDELIMITERDELIMITERDELIMITERWorld!".getBytes());
        Splitter splitter = new Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("Hello", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("World!", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterWithOnlyDelimiters() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("DELIMITERDELIMITERDELIMITER".getBytes());
        Splitter splitter = new Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterWithBufferSize4() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("HelloDELIMITERWorld!DELIMITERByeDELIMITERWorld!".getBytes());
        Splitter splitter = new Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("Hello", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 4));
        assertEquals("World!", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 4));
        assertEquals("Bye", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 4));
        assertEquals("World!", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 4));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterWithBufferSize5() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("HelloDELIMITERWorld!DELIMITERByeDELIMITERWorld!".getBytes());
        Splitter splitter = new Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("Hello", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 5));
        assertEquals("World!", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 5));
        assertEquals("Bye", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 5));
        assertEquals("World!", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 5));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterSkip() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("HelloDELIMITERWorld!DELIMITERByeDELIMITERWorld!".getBytes());
        Splitter splitter = new Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("Hello", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 5));
        assertEquals(6, splitter.getNextInputStream().orElseThrow().skip(Long.MAX_VALUE));
        assertEquals("Bye", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 5));
        assertEquals(6, splitter.getNextInputStream().orElseThrow().skip(Long.MAX_VALUE));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterWithPartialDelimiterBeforeActualDelimiter() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("HelloDDDDELIMITERWorld!".getBytes());
        Splitter splitter = new Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("HelloDDD", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertEquals("World!", readAllBytesToString(splitter.getNextInputStream().orElseThrow()));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterWithLongPartialDelimiterAndBufferSize2() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("HelloDELIMITWorld!".getBytes());
        Splitter splitter = new Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("HelloDELIMITWorld!", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 2));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterWithLongPartialDelimiterAndBufferSize3() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("HelloDELIMITWorld!".getBytes());
        Splitter splitter = new Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("HelloDELIMITWorld!", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 3));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterWithLongPartialDelimiterAndBufferSize4() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("HelloDELIMITWorld!".getBytes());
        Splitter splitter = new Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("HelloDELIMITWorld!", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 4));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }

    @Test
    void testSplitterWithLongPartialDelimiterAndBufferSize5() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("HelloDELIMITWorld!".getBytes());
        Splitter splitter = new Splitter(inputStream, "DELIMITER".getBytes());
        assertEquals("HelloDELIMITWorld!", readChunksOfBytesToString(splitter.getNextInputStream().orElseThrow(), 5));
        assertTrue(splitter.getNextInputStream().isEmpty());
    }
}
