package b.nana.technology.gingester.transformers.base.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ByteSizeParserTest {

    @Test
    void parse() {

        assertEquals(10, ByteSizeParser.parse("10"));
        assertEquals(10, ByteSizeParser.parse("10B"));

        assertEquals(1000, ByteSizeParser.parse("1KB"));
        assertEquals(1000000, ByteSizeParser.parse("1MB"));

        assertEquals(1024, ByteSizeParser.parse("1KiB"));
        assertEquals(1048576, ByteSizeParser.parse("1MiB"));

        assertEquals(1500, ByteSizeParser.parse("1.5KB"));
    }
}