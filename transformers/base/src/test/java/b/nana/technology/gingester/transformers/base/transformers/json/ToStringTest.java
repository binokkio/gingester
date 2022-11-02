package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToStringTest {

    @Test
    void testTextNodeToString() throws Exception {
        TextNode in = JsonNodeFactory.instance.textNode("Hello, World!");
        new ToString(new ToString.Parameters())
                .transform(null, in, (UniReceiver<String>) s -> assertEquals("Hello, World!", s));
    }
}