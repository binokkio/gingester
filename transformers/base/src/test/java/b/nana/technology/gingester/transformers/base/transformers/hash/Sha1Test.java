package b.nana.technology.gingester.transformers.base.transformers.hash;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class Sha1Test {

    @Test
    void test() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
                        "-t StringCreate 'Hello, World!' " +
                        "-t Sha1 " +
                        "-t Base16Encode " +
                        "-t BytesToString")
                .attach(result::set)
                .run();

        assertEquals("0a0a9f2a6772942557ab5355d76af442f8f65e01", result.get());
    }
}