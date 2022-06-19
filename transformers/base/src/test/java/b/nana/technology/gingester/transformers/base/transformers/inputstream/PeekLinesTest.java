package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PeekLinesTest {

    @Test
    public void test() {

        AtomicReference<String> peekResult = new AtomicReference<>();
        AtomicReference<String> fullResult = new AtomicReference<>();

        new Gingester().cli("" +
                "-t ResourceOpen /data/dsv/test.csv " +
                "-t PeekLines 1 " +
                "-f " +
                "-t InputStreamToString")
                .attach(peekResult::set, "PeekLines")
                .attach(fullResult::set)
                .run();

        assertEquals("a,b,c", peekResult.get());
        assertEquals("a,b,c\n1,2,3\n2,3,4\n3,4,5\n", fullResult.get());
    }
}