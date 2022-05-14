package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FetchOutputTypeTest {

    @Test
    void testBasic() {

        Gingester gingester = new Gingester().cli("" +
                "-t StringCreate 'Hello, World' " +
                "-t StringToBytes " +
                "-s -f " +
                "-t StringAppend '!'");

        AtomicReference<String> result = new AtomicReference<>();
        gingester.attach(result::set);

        gingester.run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testWithStashName() {

        Gingester gingester = new Gingester().cli("" +
                "-t StringCreate 'Hello, World' -t StringToBytes -s hello " +
                "-t StringCreate 'Bye, World' -t StringToBytes -s bye " +
                "-f hello " +
                "-t StringAppend '!'");

        AtomicReference<String> result = new AtomicReference<>();
        gingester.attach(result::set);

        gingester.run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testWithInputStasherId() {

        Gingester gingester = new Gingester().cli("" +
                "-t StringCreate 'Hello, World' -t StringToBytes -t Target:Stash " +
                "-t StringCreate 'Bye, World' -t StringToBytes -t Distraction:Stash " +
                "-f Target.stash " +
                "-t StringAppend '!'");

        AtomicReference<String> result = new AtomicReference<>();
        gingester.attach(result::set);

        gingester.run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testWithStashNameAndInputStasherId() {

        Gingester gingester = new Gingester().cli("" +
                "-t StringCreate 'Hello, World' -t StringToBytes -t Target:Stash hello " +
                "-t StringCreate 'Bye, World' -t StringToBytes -t Distraction:Stash bye " +
                "-f Target.hello " +
                "-t StringAppend '!'");

        AtomicReference<String> result = new AtomicReference<>();
        gingester.attach(result::set);

        gingester.run();

        assertEquals("Hello, World!", result.get());
    }
}
