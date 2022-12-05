package b.nana.technology.gingester.core.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ByteCountFormatTest {

    private final ByteCountFormat byteCountFormat = new ByteCountFormat();

    @Test
    void testParse10Bytes() {
        assertEquals(10, byteCountFormat.parse("10"));
        assertEquals(10, byteCountFormat.parse("10B"));
    }

    @Test
    void testParseIntegerWithDecimalMultiplier() {
        assertEquals(1000, byteCountFormat.parse("1KB"));
        assertEquals(1000000, byteCountFormat.parse("1MB"));
    }

    @Test
    void testParseIntegerWithBinaryMultiplier() {
        assertEquals(1024, byteCountFormat.parse("1KiB"));
        assertEquals(1048576, byteCountFormat.parse("1MiB"));
    }

    @Test
    void testParse1Point5KB() {
        assertEquals(1500, byteCountFormat.parse("1.5KB"));
    }

    @Test
    void testParsePoint5KiB() {
        assertEquals(512, byteCountFormat.parse(".5KiB"));
    }

    @Test
    void testFormat() {
        assertEquals("0.00B",  byteCountFormat.format(0d));
        assertEquals("0.100B", byteCountFormat.format(0.1d));
        assertEquals("1.00B",  byteCountFormat.format(1d));
        assertEquals("10.0B",  byteCountFormat.format(10d));
        assertEquals("100B",   byteCountFormat.format(100d));
        assertEquals("1.00KB", byteCountFormat.format(1000d));
        assertEquals("10.0KB", byteCountFormat.format(9999d));
        assertEquals("10.0KB", byteCountFormat.format(10000d));
        assertEquals("100KB",  byteCountFormat.format(99999d));
        assertEquals("100KB",  byteCountFormat.format(100000d));
        assertEquals("1.00MB", byteCountFormat.format(999999d));
        assertEquals("1.00MB", byteCountFormat.format(1000000d));
        assertEquals("10.0MB", byteCountFormat.format(10000000d));
        assertEquals("100MB",  byteCountFormat.format(100000000d));
        assertEquals("1.00GB", byteCountFormat.format(1000000000d));
        assertEquals("10.0GB", byteCountFormat.format(10000000000d));
        assertEquals("100GB",  byteCountFormat.format(100000000000d));
        assertEquals("1.00TB", byteCountFormat.format(1000000000000d));
        assertEquals("10.0TB", byteCountFormat.format(10000000000000d));
        assertEquals("100TB",  byteCountFormat.format(100000000000000d));
        assertEquals("1.00PB", byteCountFormat.format(1000000000000000d));
        assertEquals("10.0PB", byteCountFormat.format(10000000000000000d));
        assertEquals("100PB",  byteCountFormat.format(100000000000000000d));
        assertEquals("1.00EB", byteCountFormat.format(1000000000000000000d));
        assertEquals("10.0EB", byteCountFormat.format(10000000000000000000d));
        assertEquals("100EB",  byteCountFormat.format(100000000000000000000d));
        assertEquals("1.00ZB", byteCountFormat.format(1000000000000000000000d));
        assertEquals("10.0ZB", byteCountFormat.format(10000000000000000000000d));
        assertEquals("100ZB",  byteCountFormat.format(100000000000000000000000d));
        assertEquals("1.00YB", byteCountFormat.format(1000000000000000000000000d));
        assertEquals("10.0YB", byteCountFormat.format(10000000000000000000000000d));
        assertEquals("100YB",  byteCountFormat.format(100000000000000000000000000d));

        // since we only keep 3 significant digits and have no multiplier prefix bigger than yotta- anything
        // above 999.5YB will use scientific notation
        assertEquals("1.00E+3YB", byteCountFormat.format(999500000000000000000000000d));
    }

    @Test
    void testFormatBinary() {
        assertEquals("0.00B",    byteCountFormat.formatBinary(0d));
        assertEquals("0.100B",   byteCountFormat.formatBinary(0.1d));
        assertEquals("0.0100B",  byteCountFormat.formatBinary(0.01d));
        assertEquals("0.00100B", byteCountFormat.formatBinary(0.001d));
        assertEquals("1.00B",    byteCountFormat.formatBinary(1d));
        assertEquals("10.0B",    byteCountFormat.formatBinary(10d));
        assertEquals("100B",     byteCountFormat.formatBinary(100d));
        assertEquals("0.977KiB", byteCountFormat.formatBinary(1000d));
        assertEquals("1.00KiB",  byteCountFormat.formatBinary(1024d));
        assertEquals("9.77KiB",  byteCountFormat.formatBinary(10000d));
        assertEquals("97.7KiB",  byteCountFormat.formatBinary(100000d));
        assertEquals("977KiB",   byteCountFormat.formatBinary(1000000d));
        assertEquals("999KiB",   byteCountFormat.formatBinary(1023487d));
        assertEquals("0.976MiB", byteCountFormat.formatBinary(1023488d));
        assertEquals("1.00MiB",  byteCountFormat.formatBinary(1048576d));
        assertEquals("9.54MiB",  byteCountFormat.formatBinary(10000000d));
        assertEquals("95.4MiB",  byteCountFormat.formatBinary(100000000d));
        assertEquals("954MiB",   byteCountFormat.formatBinary(1000000000d));
        assertEquals("9.31GiB",  byteCountFormat.formatBinary(10000000000d));
        assertEquals("93.1GiB",  byteCountFormat.formatBinary(100000000000d));
        assertEquals("931GiB",   byteCountFormat.formatBinary(1000000000000d));
    }
}