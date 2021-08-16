package b.nana.technology.gingester.transformers.freemarker.transformers;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestFreemarker {

    @Test
    void testHelloFreemarkerWorld() throws IOException {
        test("test-data_hello-freemarker-world");
    }

    @Test
    void testListAndHash() throws IOException {
        test("test-data_list-and-hash");
    }


    private void test(String resourceDirectory) throws IOException {

        JsonNode input = new ObjectMapper().readTree(getClass().getResourceAsStream("/" + resourceDirectory + "/input.json"));

        Freemarker.Parameters freemarkerParameters = new Freemarker.Parameters("/" + resourceDirectory + "/template.ftl");
        Freemarker freemarker = new Freemarker(freemarkerParameters);

        AtomicReference<String> result = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(freemarker, input);
        gBuilder.link(freemarker, result::set);
        gBuilder.build().run();

        assertEquals(
                new String(Objects.requireNonNull(getClass().getResourceAsStream("/" + resourceDirectory + "/expected-result.txt")).readAllBytes()),
                result.get()
        );
    }
}
