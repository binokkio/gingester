package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.receiver.UniReceiver;
import b.nana.technology.gingester.core.template.TemplateParameters;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppendTest {

    @Test
    void testStringAppend() {
        Append.Parameters parameters = new Append.Parameters(new TemplateParameters("!"));
        Append append = new Append(parameters);
        Queue<String> result = new ArrayDeque<>();
        append.transform(null, "Hello, World", (UniReceiver<String>) result::add);
        assertEquals(1, result.size());
        assertEquals("Hello, World!", result.remove());
    }
}