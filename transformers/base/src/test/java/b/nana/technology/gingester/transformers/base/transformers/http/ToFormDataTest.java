package b.nana.technology.gingester.transformers.base.transformers.http;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ToFormDataTest {

    @Test
    void testToFormData() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t JsonDef @ \"{hello:'world a',bye:'world b'}\" " +
                "-t ToFormData")
                .add(result::set)
                .run();

        assertEquals("----------gingester-form-data-boundary\n" +
                "Content-Disposition: form-data; name=\"hello\"\n" +
                "\n" +
                "world a\n" +
                "----------gingester-form-data-boundary\n" +
                "Content-Disposition: form-data; name=\"bye\"\n" +
                "\n" +
                "world b\n" +
                "----------gingester-form-data-boundary--\n\n", result.get());
    }
}