package b.nana.technology.gingester.transformers.base.transformers.base64;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EncodeTest {

    @Test
    void testSingleLineOutput() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Base64Encode " +
                "-t BytesToString")
                .add(result::set)
                .run();

        assertEquals("SGVsbG8sIFdvcmxkIQ==", result.get());
    }

    @Test
    void testMultiLineOutput() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Base64Encode 8 " +
                "-t BytesToString")
                .add(result::set)
                .run();

        assertEquals("SGVsbG8s\r\nIFdvcmxk\r\nIQ==", result.get());
    }
}