package b.nana.technology.gingester.transformers.base.transformers.base64;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecodeTest {

    @Test
    void testSingleLine() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t StringDef 'SGVsbG8sIFdvcmxkIQ=='
                -t Base64Decode
                -t BytesToString""")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testMultiLine() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t StringDef 'SGVsbG8s\r\nIFdvcmxk\r\nIQ=='
                -t Base64Decode mime
                -t BytesToString""")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }
}