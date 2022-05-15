package b.nana.technology.gingester.core.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CliSplitterTest {

    @Test
    void testSimpleHelloWorld() {

        String[] args = CliSplitter.split("hello world");

        assertEquals(2, args.length);
        assertEquals("hello", args[0]);
        assertEquals("world", args[1]);
    }

    @Test
    void testQuotedHelloWorld() {

        String[] args = CliSplitter.split("\"hello\" \"world\"");

        assertEquals(2, args.length);
        assertEquals("hello", args[0]);
        assertEquals("world", args[1]);
    }

    @Test
    void testSimpleJsonParameters() {

        String[] args = CliSplitter.split("-t Dummy '{\"hello\": \"world\"}'");

        assertEquals(3, args.length);
        assertEquals("-t", args[0]);
        assertEquals("Dummy", args[1]);
        assertEquals("{\"hello\": \"world\"}", args[2]);
    }

    @Test
    void testJsonParametersWithEscapedQuotes() {

        String[] args = CliSplitter.split("-t Dummy \"{\\\"hello\\\": \\\"world\\\"}\"");

        assertEquals(3, args.length);
        assertEquals("-t", args[0]);
        assertEquals("Dummy", args[1]);
        assertEquals("{\"hello\": \"world\"}", args[2]);
    }
}