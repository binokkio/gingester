package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FetchOutputTypeTest {

    @Test
    void testBasic() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t StringDef 'Hello, World'
                -t StringToBytes
                -s -f
                -t StringAppend '!'
                """)
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testWithStashName() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t StringDef 'Hello, World' -t StringToBytes -s hello
                -t StringDef 'Bye, World' -t StringToBytes -s bye
                -f hello
                -t StringAppend '!'
                """)
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testWithInputStasherId() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t StringDef 'Hello, World' -t StringToBytes -t Target:Stash
                -t StringDef 'Bye, World' -t StringToBytes -t Distraction:Stash
                -f Target.stash
                -t StringAppend '!'
                """)
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testWithStashNameAndInputStasherId() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t StringDef 'Hello, World' -t StringToBytes -t Target:Stash hello
                -t StringDef 'Bye, World' -t StringToBytes -t Distraction:Stash bye
                -f Target.hello
                -t StringAppend '!'
                """)
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }
}
