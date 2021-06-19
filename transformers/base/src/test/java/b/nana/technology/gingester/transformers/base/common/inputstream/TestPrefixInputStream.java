package b.nana.technology.gingester.transformers.base.common.inputstream;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static b.nana.technology.gingester.transformers.base.common.inputstream.Helpers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestPrefixInputStream {

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
}
