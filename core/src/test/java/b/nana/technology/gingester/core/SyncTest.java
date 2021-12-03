package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncTest {

    @Test
    void testOnPrepare() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-sft Generate \"Hello, World!\" " +
                "-stt OnPrepare " +
                "-t Generate 'Message from OnPrepare'");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from OnPrepare", result.get());
    }

    @Test
    void testOnFinish() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-sft Generate \"Hello, World!\" " +
                "-stt OnFinish " +
                "-t Generate 'Message from OnFinish'");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testPrepareOrder() {

        AtomicReference<Integer> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-sft Generate \"Hello, World!\" " +
                "-stt OnPrepare " +
                "-stt SyncCounter");

        gingester.add(result::set);

        gingester.run();

        assertEquals(1, result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation1() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-t Monkey 1 " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish'");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation2() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-t Monkey 1 -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish'");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation3() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 " +
                "-s " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish' " +
                "-s");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation4() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 -- " +
                "-s " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish' " +
                "-s");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation5() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 " +
                "-s -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish' " +
                "-s");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation6() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-s " +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-s " +
                "-t Monkey 1 -- " +
                "-s -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish' " +
                "-s");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }

    @Test
    void testSyncThroughExceptionHandlerVariation7() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-s " +
                "-e ExceptionHandler " +
                "-t Generate \"Hello, World!\" " +
                "-sf Generate " +
                "-t Monkey 1 -- " +
                "-s -- " +
                "-stt ExceptionHandler:OnFinish " +
                "-t Generate 'Message from OnFinish' " +
                "-s");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from OnFinish", result.get());
    }
}
