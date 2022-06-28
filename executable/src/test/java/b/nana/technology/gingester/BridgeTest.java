package b.nana.technology.gingester;

import b.nana.technology.gingester.core.Gingester;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class BridgeTest {

    @Test
    void testShortBridge() throws IOException {

        Gingester gingester = new Gingester().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Pack greeting.txt -t Compress " +
                "-t InputStreamToBytes");

        AtomicReference<byte[]> result = new AtomicReference<>();
        gingester.attach(result::set);

        gingester.run();

        TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(result.get())));
        TarArchiveEntry entry = tar.getNextTarEntry();

        assertEquals("greeting.txt", entry.getName());
        assertEquals("Hello, World!", new String(tar.readAllBytes()));
    }

    @Test
    void testShortBridgeWithPassthroughs() throws IOException {

        Gingester gingester = new Gingester().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-s -t Passthrough " +
                "-t Pack greeting.txt -t Compress " +
                "-t InputStreamToBytes");

        AtomicReference<byte[]> result = new AtomicReference<>();
        gingester.attach(result::set);

        gingester.run();

        TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(result.get())));
        TarArchiveEntry entry = tar.getNextTarEntry();

        assertEquals("greeting.txt", entry.getName());
        assertEquals("Hello, World!", new String(tar.readAllBytes()));
    }

    @Test
    void testLongBridge() {

        Gingester gingester = new Gingester().cli("" +
                "-t JsonDef '{hello:1,world:2}' " +
                "-t InputStreamAppend '!!!' " +
                "-t InputStreamToString");

        AtomicReference<String> result = new AtomicReference<>();
        gingester.attach(result::set);

        gingester.run();

        assertEquals("{\"hello\":1,\"world\":2}!!!", result.get());
    }

    @Test
    void testLongBridgeWithPassthroughs() {

        Gingester gingester = new Gingester().cli("" +
                "-t JsonDef '{hello:1,world:2}' " +
                "-s -t Passthrough " +
                "-t InputStreamAppend '!!!' " +
                "-t InputStreamToString");

        AtomicReference<String> result = new AtomicReference<>();
        gingester.attach(result::set);

        gingester.run();

        assertEquals("{\"hello\":1,\"world\":2}!!!", result.get());
    }

    @Test
    void testNoBridgingSolutionFoundThrows() {

        Gingester gingester = new Gingester().cli("" +
                "-t TimeNow " +
                "-t PathSize");

        IllegalStateException e = assertThrows(IllegalStateException.class, gingester::run);
        assertEquals("Transformations from TimeNow to PathSize must be specified", e.getMessage());
    }
}
