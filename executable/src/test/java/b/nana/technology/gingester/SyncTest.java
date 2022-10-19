package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

class SyncTest {

    @Test
    void testSyncBeforeJoin() {

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-sft Repeat 100000 " +
                "-stt GroupByEquals " +
                "-t InputStreamJoin " +
                "-t InputStreamDrain")
                .run();
    }
}
