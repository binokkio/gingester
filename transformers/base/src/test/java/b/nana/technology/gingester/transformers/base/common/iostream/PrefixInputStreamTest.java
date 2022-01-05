package b.nana.technology.gingester.transformers.base.common.iostream;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static b.nana.technology.gingester.transformers.base.common.iostream.Helpers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefixInputStreamTest {

    @Test
    void testPrefixInputStreamReadAllBytes() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("World!".getBytes());
        PrefixInputStream prefixInputStream = new PrefixInputStream(byteArrayInputStream);
        prefixInputStream.prefix(", ".getBytes());
        prefixInputStream.prefix("Hello".getBytes());
        assertEquals("Hello, World!", readAllBytesToString(prefixInputStream));
    }

    @Test
    void testPrefixInputStreamReadSingleBytes() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("World!".getBytes());
        PrefixInputStream prefixInputStream = new PrefixInputStream(byteArrayInputStream);
        prefixInputStream.prefix(", ".getBytes());
        prefixInputStream.prefix("Hello".getBytes());
        assertEquals("Hello, World!", readSingleBytesToString(prefixInputStream));
    }

    @Test
    void testPrefixInputStreamReadWithBufferSize4() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("World!".getBytes());
        PrefixInputStream prefixInputStream = new PrefixInputStream(byteArrayInputStream);
        prefixInputStream.prefix(", ".getBytes());
        prefixInputStream.prefix("Hello".getBytes());
        assertEquals("Hello, World!", readChunksOfBytesToString(prefixInputStream, 4));
    }

    @Test
    void testPrefixInputStreamReadWithBufferSize5() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("World!".getBytes());
        PrefixInputStream prefixInputStream = new PrefixInputStream(byteArrayInputStream);
        prefixInputStream.prefix(", ".getBytes());
        prefixInputStream.prefix("Hello".getBytes());
        assertEquals("Hello, World!", readChunksOfBytesToString(prefixInputStream, 5));
    }

    @Test
    void testSmallPrefixBugFix() throws IOException {

        byte[] bytes = "Hello, World!".getBytes();
        PrefixInputStream prefixInputStream = new PrefixInputStream(new ByteArrayInputStream(bytes));
        prefixInputStream.setMinimumBufferSize(8);
        prefixInputStream.prefix(new byte[] { ' ' });  // prefix length is smaller than minimum buffer size

        byte[] buffer = new byte[bytes.length + 1];
        int read = prefixInputStream.read(buffer);
        assertEquals(14, read);
        assertEquals(" Hello, World!", new String(buffer));
        assertTrue(prefixInputStream.getBuffer().length >= 8);  // prefix was not returned as buffer
    }
}
