package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SyncTest {

    @Test
    void testOnPrepare() {

        AtomicReference<String> result = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-sft StringDef \"Hello, World!\" " +
                "-stt OnPrepare " +
                "-t StringDef 'Message from OnPrepare'");

        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals("Message from OnPrepare", result.get());
    }

    @Test
    void testOnFinish() {

        AtomicReference<String> result = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-sft StringDef \"Hello, World!\" " +
                "-stt OnFinish " +
                "-t StringDef 'Message from OnFinish'");

        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testPrepareOrder() {

        AtomicReference<Integer> result = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-sft StringDef \"Hello, World!\" " +
                "-stt OnPrepare " +
                "-stt SyncCounter");

        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals(1, result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation1() {

        AtomicReference<String> result = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-e ExceptionHandler " +
                "-sft StringDef \"Hello, World!\" " +
                "-t Throw 'Exception!' -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t StringDef 'Message from OnFinish'");

        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation2() {

        AtomicReference<String> result = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft StringDef \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 " +
                "-s -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t StringDef 'Message from OnFinish' " +
                "-s");

        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation3() {

        AtomicReference<String> result = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft StringDef \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 -- " +
                "-s " +
                "-stt ExceptionHandler:OnFinish " +
                "-t StringDef 'Message from OnFinish' " +
                "-s");

        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation4() {

        AtomicReference<String> result = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft StringDef \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 " +
                "-s -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t StringDef 'Message from OnFinish' " +
                "-s");

        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation5() {

        AtomicReference<String> result = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft StringDef \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 -- " +
                "-s -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t StringDef 'Message from OnFinish' " +
                "-s");

        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation7() {

        AtomicReference<String> result = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-s " +
                "-e ExceptionHandler " +
                "-t StringDef \"Hello, World!\" " +
                "-sf StringDef " +
                "-t Monkey 1 -- " +
                "-s -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t StringDef 'Message from OnFinish' " +
                "-s");

        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testDiamondSyncFinish() {

        AtomicReference<String> result = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-sft StringDef \"Hello, World!\" " +
                "-l A B " +
                "-t A:Stash -l C " +
                "-t B:Stash -l C " +
                "-stt C:OnFinish " +
                "-t StringDef 'Message from OnFinish'");

        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandler() {

        AtomicReference<String> result = new AtomicReference<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-e C " +
                "-sft StringDef \"Hello, World!\" " +
                "-l A B " +
                "-t A:Stash -- " +
                "-t B:Stash -- " +
                "-stt C:OnFinish " +
                "-t StringDef 'Message from OnFinish'");

        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncToUnawareTransformerThrows() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-stt StringDef hello");

        Exception e = assertThrows(IllegalArgumentException.class, flowBuilder::run);
        assertEquals("StringDef is synced with __seed__ but is not sync-aware", e.getMessage());
    }
}
