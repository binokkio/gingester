package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncTest {

    @Test
    void testOnPrepare() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester("" +
                "-sft Generate \"Hello, World!\" " +
                "-stt OnPrepare " +
                "-t Generate 'Message from OnPrepare'");

        gingester.attach(result::set);

        gingester.run();

        assertEquals("Message from OnPrepare", result.get());
    }

    @Test
    void testOnFinish() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester("" +
                "-sft Generate \"Hello, World!\" " +
                "-stt OnFinish " +
                "-t Generate 'Message from OnFinish'");

        gingester.attach(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testPrepareOrder() {

        AtomicReference<Integer> result = new AtomicReference<>();

        Gingester gingester = new Gingester("" +
                "-sft Generate \"Hello, World!\" " +
                "-stt OnPrepare " +
                "-stt SyncCounter");

        gingester.attach(result::set);

        gingester.run();

        assertEquals(1, result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation1() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester("" +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-t Throw 'Exception!' -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish'");

        gingester.attach(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation2() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 " +
                "-s -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish' " +
                "-s");

        gingester.attach(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation3() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 -- " +
                "-s " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish' " +
                "-s");

        gingester.attach(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation4() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 " +
                "-s -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish' " +
                "-s");

        gingester.attach(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation5() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 -- " +
                "-s -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish' " +
                "-s");

        gingester.attach(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation7() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester("" +
                "-s " +
                "-e ExceptionHandler " +
                "-t Generate \"Hello, World!\" " +
                "-sf Generate " +
                "-t Monkey 1 -- " +
                "-s -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish' " +
                "-s");

        gingester.attach(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testDiamondSyncFinish() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester("" +
                "-sft Generate \"Hello, World!\" " +
                "-l A B " +
                "-t A:Stash -l C " +
                "-t B:Stash -l C " +
                "-stt C:OnFinish " +
                "-t Generate 'Message from OnFinish'");

        gingester.attach(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandler() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester("" +
                "-e C " +
                "-sft Generate \"Hello, World!\" " +
                "-l A B " +
                "-t A:Stash -- " +
                "-t B:Stash -- " +
                "-stt C:OnFinish " +
                "-t Generate 'Message from OnFinish'");

        gingester.attach(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }
}
