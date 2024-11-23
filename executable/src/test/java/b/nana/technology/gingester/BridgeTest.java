package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
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

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Pack greeting.txt -t Compress " +
                "-t InputStreamToBytes");

        AtomicReference<byte[]> result = new AtomicReference<>();
        flowBuilder.add(result::set);

        flowBuilder.run();

        TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(result.get())));
        TarArchiveEntry entry = tar.getNextTarEntry();

        assertEquals("greeting.txt", entry.getName());
        assertEquals("Hello, World!", new String(tar.readAllBytes()));
    }

    @Test
    void testShortBridgeWithPassthroughs() throws IOException {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-s -t Passthrough " +
                "-t Pack greeting.txt -t Compress " +
                "-t InputStreamToBytes");

        AtomicReference<byte[]> result = new AtomicReference<>();
        flowBuilder.add(result::set);

        flowBuilder.run();

        TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(result.get())));
        TarArchiveEntry entry = tar.getNextTarEntry();

        assertEquals("greeting.txt", entry.getName());
        assertEquals("Hello, World!", new String(tar.readAllBytes()));
    }

    @Test
    void testLongBridge() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t JsonDef @ '{hello:1,world:2}' " +
                "-t InputStreamAppend '!!!' " +
                "-t InputStreamToString");

        AtomicReference<String> result = new AtomicReference<>();
        flowBuilder.add(result::set);

        flowBuilder.run();

        assertEquals("{\"hello\":1,\"world\":2}!!!", result.get());
    }

    @Test
    void testLongBridgeWithPassthroughs() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t JsonDef @ '{hello:1,world:2}' " +
                "-s -t Passthrough " +
                "-t InputStreamAppend '!!!' " +
                "-t InputStreamToString");

        AtomicReference<String> result = new AtomicReference<>();
        flowBuilder.add(result::set);

        flowBuilder.run();

        assertEquals("{\"hello\":1,\"world\":2}!!!", result.get());
    }

    @Test
    void testNoBridgingSolutionFoundThrows1() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t TimeNow " +
                "-t PathSize");

        IllegalStateException e = assertThrows(IllegalStateException.class, flowBuilder::run);
        assertEquals("Transformations from TimeNow to PathSize must be specified", e.getMessage());
    }

    @Test
    void testNoBridgingSolutionFoundThrows2() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t StringDef hello " +
                "-t PathMove /tmp/foo");

        IllegalStateException e = assertThrows(IllegalStateException.class, flowBuilder::run);
        assertEquals("Transformations from StringDef to PathMove must be specified", e.getMessage());
    }

    @Test
    void testDynamicBridge1() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t Gcli @ '-t StringDef "Hello, World!" -t ListCollect'
                -t JsonWrap 'greetings'
                --as String
                """)
                .add(result::set)
                .run();

        assertEquals("{\"greetings\":[\"Hello, World!\"]}", result.get());
    }

    @Test
    void testDynamicBridge2() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t JsonDef @ '{foo:{bar:123}}'
                -s json
                -f json.foo.bar
                -t StringAppend '!'
                """)
                .add(result::set)
                .run();

        assertEquals("123!", result.get());
    }
}
