package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.batch.Item;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.BiReceiver;
import b.nana.technology.gingester.transformers.jetty.http.Split;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplitTest {

    @Test
    void testFileOnly() throws Exception {

        Context context = Context.newTestContext()
                .stash(Map.of("http", Map.of("request", Map.of("headers", Map.of(
                        "Content-Type", "multipart/form-data; boundary=---------------------------17192798713081016645112320327"
                ))))).buildForTesting();

        AtomicReference<Item<byte[]>> result = new AtomicReference<>();

        new Split().transform(
                context,
                getClass().getResourceAsStream("/hello-world.multipart-formdata"),
                (BiReceiver<InputStream>) (c, i) -> {
                    try {
                        result.set(new Item<>(c, i.readAllBytes()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        assertEquals("file", result.get().getContext().fetch(new FetchKey("name")).findFirst().orElseThrow());
        assertEquals("hello-world.txt", result.get().getContext().fetch(new FetchKey("filename")).findFirst().orElseThrow());
        assertEquals("Hello, World!", new String(result.get().getValue(), StandardCharsets.UTF_8));
    }

    @Test
    void testFileWithMetaData() throws Exception {

        Context context = Context.newTestContext()
                .stash(Map.of("http", Map.of("request", Map.of("headers", Map.of(
                        "Content-Type", "multipart/form-data; boundary=---------------------------403540100931368458281198214153"
                ))))).buildForTesting();

        List<Item<byte[]>> results = new ArrayList<>();

        new Split().transform(
                context,
                getClass().getResourceAsStream("/hello-world-with-metadata.multipart-formdata"),
                (BiReceiver<InputStream>) (c, i) -> {
                    try {
                        results.add(new Item<>(c, i.readAllBytes()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        assertEquals(3, results.size());

        Item<byte[]> hello = results.get(0);
        assertEquals("hello", hello.getContext().fetch(new FetchKey("name")).findFirst().orElseThrow());
        assertEquals("world", new String(hello.getValue()));

        Item<byte[]> bye = results.get(1);
        assertEquals("bye", bye.getContext().fetch(new FetchKey("name")).findFirst().orElseThrow());
        assertEquals("world", new String(bye.getValue()));

        Item<byte[]> file = results.get(2);
        assertEquals("file", file.getContext().fetch(new FetchKey("name")).findFirst().orElseThrow());
        assertEquals("hello-world.txt", file.getContext().fetch(new FetchKey("filename")).findFirst().orElseThrow());
        assertEquals("Hello, World!", new String(file.getValue()));
    }
}
