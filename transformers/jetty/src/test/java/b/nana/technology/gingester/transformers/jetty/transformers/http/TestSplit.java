package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.Item;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToString;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestSplit {

    @Test
    void testFileOnly() {

        Split split = new Split();
        ToString toString = new ToString();
        AtomicReference<Item<String>> result = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(
                split,
                Context.newBuilder().stash(Map.of(
                        "headers", Map.of(
                                "Content-Type", "multipart/form-data; boundary=---------------------------17192798713081016645112320327"
                        )
                )),
                getClass().getResourceAsStream("/hello-world.multipart-formdata")
        );
        gBuilder.link(split, toString);
        gBuilder.link(toString, (c, v) -> result.set(new Item<>(c, v)));
        gBuilder.build().run();

        assertEquals("file", result.get().getContext().fetch("name").orElseThrow());
        assertEquals("hello-world.txt", result.get().getContext().fetch("filename").orElseThrow());
        assertEquals("Hello, World!", result.get().getValue());
    }

    @Test
    void testFileWithMetaData() {

        Split split = new Split();
        ToString toString = new ToString();

        List<Item<String>> results = new ArrayList<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(
                split,
                Context.newBuilder().stash(Map.of(
                        "headers", Map.of(
                                "Content-Type", "multipart/form-data; boundary=---------------------------403540100931368458281198214153"
                        )
                )),
                getClass().getResourceAsStream("/hello-world-with-metadata.multipart-formdata")
        );
        gBuilder.link(split, toString);
        gBuilder.link(toString, (c, v) -> results.add(new Item<>(c, v)));
        gBuilder.build().run();

        assertEquals(3, results.size());

        Item<String> hello = results.get(0);
        assertEquals("hello", hello.getContext().fetch("name").orElseThrow());
        assertEquals("world", hello.getValue());

        Item<String> bye = results.get(1);
        assertEquals("bye", bye.getContext().fetch("name").orElseThrow());
        assertEquals("world", bye.getValue());

        Item<String> file = results.get(2);
        assertEquals("file", file.getContext().fetch("name").orElseThrow());
        assertEquals("hello-world.txt", file.getContext().fetch("filename").orElseThrow());
        assertEquals("Hello, World!", file.getValue());
    }
}
