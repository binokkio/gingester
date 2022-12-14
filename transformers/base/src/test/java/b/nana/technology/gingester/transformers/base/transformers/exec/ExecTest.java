package b.nana.technology.gingester.transformers.base.transformers.exec;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ExecTest {

    @Test
    void testCommandString() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Exec '/bin/cat' " +
                "-t InputStreamToString")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testCommandTemplate() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Exec '{template: \"/bin/cat\", is: \"STRING\"}' " +
                "-t InputStreamToString")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testCommandStringWithWorkDir() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Exec '{command: \"/bin/cat\", workDir: \"/\"}' " +
                "-t InputStreamToString")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testCommandTemplateWithWorkDir() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Exec '{command: {template: \"/bin/cat\", is: \"STRING\"}, workDir: \"/\"}' " +
                "-t InputStreamToString")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }
}