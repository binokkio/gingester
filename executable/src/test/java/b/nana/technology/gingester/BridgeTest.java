package b.nana.technology.gingester;

import b.nana.technology.gingester.core.Gingester;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BridgeTest {

    @Test
    void testShortBridge() throws IOException {

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-t String.Create 'Hello, World!' " +
                "-t Pack greeting.txt " +
                "-t InputStream.ToBytes");

        AtomicReference<byte[]> result = new AtomicReference<>();
        gingester.add(result::set);

        gingester.run();

        TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(result.get())));
        TarArchiveEntry entry = tar.getNextTarEntry();

        assertEquals("greeting.txt", entry.getName());
        assertEquals("Hello, World!", new String(tar.readAllBytes()));
    }

    @Test
    void testShortBridgeWithPassthroughs() throws IOException {

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-t String.Create 'Hello, World!' " +
                "-s -t Throttle " +
                "-t Pack greeting.txt " +
                "-t InputStream.ToBytes");

        AtomicReference<byte[]> result = new AtomicReference<>();
        gingester.add(result::set);

        gingester.run();

        TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(result.get())));
        TarArchiveEntry entry = tar.getNextTarEntry();

        assertEquals("greeting.txt", entry.getName());
        assertEquals("Hello, World!", new String(tar.readAllBytes()));
    }

    @Test
    void testLongBridges() {

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-t Json.Create '{hello:1,world:2}' " +
                "-t InputStream.Append '!!!' " +
                "-t String.Append '!!!'");

        AtomicReference<String> result = new AtomicReference<>();
        gingester.add(result::set);

        gingester.run();

        assertEquals("{\"hello\":1,\"world\":2}!!!!!!", result.get());
    }
}
