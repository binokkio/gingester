package b.nana.technology.gingester;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

class SyncTest {

    @Test
    void testSyncBeforeJoin() {

        new Gingester().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-sft Repeat 100000 " +
                "-stt Passthrough " +
                "-t InputStreamJoin " +
                "-t InputStreamDrain")
                .run();
    }
}
