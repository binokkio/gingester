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
    void testCommandStringWithWorkDirArray() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Exec '/bin/cat' '/' " +
                "-t InputStreamToString")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testCommandStringWithWorkDirObject() {

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
    void testCommandTemplateWithWorkDirArray() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Exec '{template: \"/bin/cat\", is: \"STRING\"}' '/' " +
                "-t InputStreamToString")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testCommandTemplateWithWorkDirObject() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Exec '{command: {template: \"/bin/cat\", is: \"STRING\"}, workDir: \"/\"}' " +
                "-t InputStreamToString")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testNoStdIn() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Exec !stdin /bin/cat " +
                "-t InputStreamToString")
                .add(result::set)
                .run();

        assertEquals("", result.get());
    }

    @Test
    void testIgnoreStdOut() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t Exec !stdin '/bin/dd bs=1024 count=1024 if=/dev/zero' . IGNORE IGNORE")
                .add(result::set)
                .run();

        assertEquals("exec finish signal", result.get());
    }
}