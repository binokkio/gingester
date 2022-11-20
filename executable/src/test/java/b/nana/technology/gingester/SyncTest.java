package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.junit.jupiter.api.Assertions.*;

class SyncTest {

    @Test
    void testSyncBeforeJoin() {

        // there was an issue where the below construction caused a deadlock, so if this unit test does not
        // run into a deadlock the issue remains solved

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-sft Repeat 100000 " +
                "-stt GroupByEquals " +
                "-t InputStreamJoin " +
                "-t InputStreamDrain")
                .run();
    }

    @Test
    void testFinishSignalsDoNotOvertake() {

        Deque<Long> results = new ConcurrentLinkedDeque<>();

        new FlowBuilder().cli("" +
                "-t A:Repeat 100 " +
                "-t B:Repeat 100 " +
                "-t 10 Passthrough " +
                "-t 10 Passthrough " +
                "-t 10 Passthrough " +
                "-sf B -stt OnFinish " +
                "-sf A -stt CountCollect")
                .add(results::add)
                .run();

        assertEquals(100, results.size());

        for (Long result : results) {
            assertEquals(100, result);
        }
    }
}
