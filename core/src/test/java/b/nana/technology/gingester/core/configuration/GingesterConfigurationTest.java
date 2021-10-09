package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GingesterConfigurationTest {

    @Test
    void testEmphasizeQuestionMinimal() throws IOException {

        Gingester gingester = new Gingester();

        GingesterConfiguration
                .fromJson(getClass().getResourceAsStream("/hello-world-emphasize-question-minimal.json"))
                .applyTo(gingester);

        Queue<String> results = new ArrayDeque<>();
        gingester.add(results::add);
        gingester.run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!?", results.remove());
    }

    @Test
    void testEmphasizeQuestionVerbose() throws IOException {

        Gingester gingester = new Gingester();

        GingesterConfiguration
                .fromJson(getClass().getResourceAsStream("/hello-world-emphasize-question-verbose.json"))
                .applyTo(gingester);

        Queue<String> results = new ArrayDeque<>();
        gingester.add(results::add);
        gingester.run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!?", results.remove());
    }

    @Test
    void testDiamond() throws IOException {

        Gingester gingester = new Gingester();

        GingesterConfiguration
                .fromJson(getClass().getResourceAsStream("/hello-world-diamond.json"))
                .applyTo(gingester);

        Queue<String> results = new ArrayDeque<>();
        gingester.add(results::add);
        gingester.run();

        assertEquals(2, results.size());
        assertEquals("Hello, World!", results.remove());
        assertEquals("Hello, World?", results.remove());
    }
}