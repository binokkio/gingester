package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FetchOutputTypeTest {

    @Test
    void testBasic() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World' " +
                "-t StringToBytes " +
                "-s -f " +
                "-t StringAppend '!'");

        AtomicReference<String> result = new AtomicReference<>();
        flowBuilder.add(result::set);

        flowBuilder.run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testWithStashName() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World' -t StringToBytes -s hello " +
                "-t StringDef 'Bye, World' -t StringToBytes -s bye " +
                "-f hello " +
                "-t StringAppend '!'");

        AtomicReference<String> result = new AtomicReference<>();
        flowBuilder.add(result::set);

        flowBuilder.run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testWithInputStasherId() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World' -t StringToBytes -t Target:Stash " +
                "-t StringDef 'Bye, World' -t StringToBytes -t Distraction:Stash " +
                "-f Target.stash " +
                "-t StringAppend '!'");

        AtomicReference<String> result = new AtomicReference<>();
        flowBuilder.add(result::set);

        flowBuilder.run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testWithStashNameAndInputStasherId() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World' -t StringToBytes -t Target:Stash hello " +
                "-t StringDef 'Bye, World' -t StringToBytes -t Distraction:Stash bye " +
                "-f Target.hello " +
                "-t StringAppend '!'");

        AtomicReference<String> result = new AtomicReference<>();
        flowBuilder.add(result::set);

        flowBuilder.run();

        assertEquals("Hello, World!", result.get());
    }
}
