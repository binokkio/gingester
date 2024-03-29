package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class NdjsonTest {

    @Test
    void test() throws IOException {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t JsonDef @ '{hello:\"world\"}' " +
                "-t Repeat 3 " +
                "-t InputStreamJoin " +
                "-t InputStreamAppend " +
                "-t Compress " +
                "-t InputStreamToBytes");

        AtomicReference<byte[]> result = new AtomicReference<>();
        flowBuilder.add(result::set);
        flowBuilder.run();

        String test = new String(new GZIPInputStream(new ByteArrayInputStream(result.get())).readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("{\"hello\":\"world\"}\n{\"hello\":\"world\"}\n{\"hello\":\"world\"}\n", test);
    }
}
