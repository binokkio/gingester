package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsTextTest {

    @Test
    void testTextNodeAsText() {
        TextNode in = JsonNodeFactory.instance.textNode("Hello, World!");
        new AsText().transform(null, in, (UniReceiver<String>) s -> assertEquals("Hello, World!", s));
    }

    @Test
    void testNumberNodeAsText() {
        NumericNode in = JsonNodeFactory.instance.numberNode(123);
        new AsText().transform(null, in, (UniReceiver<String>) s -> assertEquals("123", s));
    }
}