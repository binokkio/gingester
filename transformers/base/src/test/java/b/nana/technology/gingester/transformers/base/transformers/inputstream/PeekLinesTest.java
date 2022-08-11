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
                .attach(fullResult::set, "InputStreamToString")
                .run();

        assertEquals("a,b,c", peekResult.get());
        assertEquals("a,b,c\n1,2,3\n2,3,4\n3,4,5\n", fullResult.get());
    }

    @Test
    public void testMultiByteUtf8() {

        AtomicReference<String> peekResult = new AtomicReference<>();
        AtomicReference<String> fullResult = new AtomicReference<>();

        new Gingester().cli("" +
                "-t ResourceOpen /data/dsv/iso-8859-1.csv " +
                "-t PeekLines '{lines: 2, bufferSize: 10, charset: \"ISO-8859-1\"}' " +
                "-f " +
                "-t InputStreamToString")
                .attach(peekResult::set, "PeekLines")
                .attach(fullResult::set)
                .run();

        assertEquals("id,character\n1,ä", peekResult.get());
    }
}