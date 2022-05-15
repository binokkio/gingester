package b.nana.technology.gingester.transformers.base.transformers.primitive;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.*;

class CollectCountTest {

    @Test
    void test() {

        ArrayDeque<Long> result = new ArrayDeque<>();

        new Gingester().cli("" +
                "-sft Repeat 2 " +
                "-t Repeat 1000 " +
                "-stt CollectCount")
                .attach(result::add)
                .run();

        assertEquals(2, result.size());
        assertEquals(1000, result.remove());
        assertEquals(1000, result.remove());
    }
}