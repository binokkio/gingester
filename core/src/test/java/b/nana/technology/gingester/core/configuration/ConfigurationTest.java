package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationTest {

    @Test
    void testEmphasizeQuestionMinimal() throws IOException {

        Gingester gingester = new Gingester();

        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/hello-world-emphasize-question-minimal.json"));
        configuration.applyTo(gingester);

        Queue<String> results = new ArrayDeque<>();
        gingester.add(results::add);

        gingester.run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!?", results.remove());
    }

    @Test
    void testEmphasizeQuestionVerbose() throws IOException {

        Gingester gingester = new Gingester();

        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/hello-world-emphasize-question-verbose.json"));
        configuration.applyTo(gingester);

        Queue<String> results = new ArrayDeque<>();
        gingester.add(results::add);

        gingester.run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!?", results.remove());
    }

    @Test
    void testDiamond() throws IOException {

        Gingester gingester = new Gingester();

        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/hello-world-diamond.json"));
        configuration.applyTo(gingester);

        Parameters resultsCollectorParameters = new Parameters();
        resultsCollectorParameters.setId("ResultsCollector");

        Queue<String> results = new ArrayDeque<>();
        gingester.add(results::add, resultsCollectorParameters);

        gingester.run();

        assertEquals(2, results.size());
        assertEquals("Hello, World!", results.remove());
        assertEquals("Hello, World?", results.remove());
    }
}