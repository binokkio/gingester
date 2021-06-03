package b.nana.technology.gingester.core;

import b.nana.technology.gingester.test.transformers.Emphasize;
import b.nana.technology.gingester.test.transformers.Question;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestConfiguration {

    @Test
    void testTransformersGetUniqueNames() {
        Builder builder = new Builder();
        builder.add(new Emphasize());
        builder.add(new Emphasize());
        builder.add(new Question());
        builder.add(new Question());
        String configuration = builder.build().toConfiguration().toJson();
        assertTrue(configuration.contains("Emphasize-1"));
        assertTrue(configuration.contains("Emphasize-2"));
        assertTrue(configuration.contains("Question-1"));
        assertTrue(configuration.contains("Question-2"));
    }

    @Test
    void testHelloWorldEmphasizeQuestionMinimal() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        Builder gBuilder = Configuration.fromJson(getClass().getResourceAsStream("/hello-world-emphasize-question-minimal.json")).toBuilder();
        gBuilder.link(gBuilder.getTransformer("Question", Question.class), result::set);
        gBuilder.build().run();
        assertEquals("Hello, World!?", result.get());
    }

    @Test
    void testHelloWorldEmphasizeQuestionVerbose() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        Builder gBuilder = Configuration.fromJson(getClass().getResourceAsStream("/hello-world-emphasize-question-verbose.json")).toBuilder();
        gBuilder.link(gBuilder.getTransformer("AddQuestion", Question.class), result::set);
        gBuilder.build().run();
        assertEquals("Hello, World!?", result.get());
    }

    @Test
    void testHelloWorldDiamond() throws IOException {
        Queue<String> results = new LinkedBlockingQueue<>();
        Builder gBuilder = Configuration.fromJson(getClass().getResourceAsStream("/hello-world-diamond.json")).toBuilder();
        gBuilder.link(gBuilder.getTransformer("Emphasize", Emphasize.class), results::add);
        gBuilder.link(gBuilder.getTransformer("Question", Question.class), results::add);
        gBuilder.build().run();
        assertEquals("Hello, World!", results.remove());
        assertEquals("Hello, World?", results.remove());
    }

    @Test
    void testHelloException() throws IOException {
        Queue<String> results = new LinkedBlockingQueue<>();
        Builder gBuilder = Configuration.fromJson(getClass().getResourceAsStream("/hello-exception.json")).toBuilder();
        gBuilder.link(gBuilder.getTransformer("Emphasize", Emphasize.class), results::add);
        gBuilder.build().run();
        assertEquals("ExceptionThrower throws!", results.remove());
        assertTrue(results.isEmpty());
    }
}
