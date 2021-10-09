package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncTest {

    @Test
    void testSyncPrepare() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-sft Generate \"Hello, World!\" " +
                "-stt SyncPrepare");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from SyncPrepare prepare()", result.get());
    }

    @Test
    void testSyncFinish() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-sft Generate \"Hello, World!\" " +
                "-stt SyncFinish");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from SyncFinish finish()", result.get());
    }

//    @Test
    void testPrepareOrder() {

        AtomicReference<Integer> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-sft Generate \"Hello, World!\" " +
                "-stt SyncPrepare " +
                "-stt SyncCounter");

        gingester.add(result::set);

        gingester.run();

        assertEquals(1, result.get());
    }

    @Test
    void testSyncThroughExceptionHandler() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-t Monkey 1 " +
                "-stt ExceptionHandler:SyncFinish");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from SyncFinish finish()", result.get());
    }

    @Test
    void testSyncThroughExceptionHandler2() {

        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-e ExceptionHandler " +
                "-sft Generate \"Hello, World!\" " +
                "-t Monkey 1 -- " +
                "-stt ExceptionHandler:SyncFinish");

        gingester.add(result::set);

        gingester.run();

        assertEquals("Message from SyncFinish finish()", result.get());
    }
}
