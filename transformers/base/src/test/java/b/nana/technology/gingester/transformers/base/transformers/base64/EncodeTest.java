package b.nana.technology.gingester.transformers.base.transformers.base64;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EncodeTest {

    @Test
    void testSingleLineOutput() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t StringCreate 'Hello, World!' " +
                "-t Base64Encode " +
                "-t BytesToString")
                .attach(result::set)
                .run();

        assertEquals("SGVsbG8sIFdvcmxkIQ==", result.get());
    }

    @Test
    void testMultiLineOutput() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t StringCreate 'Hello, World!' " +
                "-t Base64Encode 8 " +
                "-t BytesToString")
                .attach(result::set)
                .run();

        assertEquals("SGVsbG8s\r\nIFdvcmxk\r\nIQ==", result.get());
    }
}