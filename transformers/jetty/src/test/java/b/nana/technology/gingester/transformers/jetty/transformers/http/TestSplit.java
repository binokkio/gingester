package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToString;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestSplit {

    @Test
    void test() {

        Split split = new Split();
        ToString toString = new ToString();

        List<Context> contexts = new ArrayList<>();
        List<String> values = new ArrayList<>();

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
        gBuilder.link(toString, (context, value) -> {
                contexts.add(context);
                values.add(value);
        });
        gBuilder.build().run();

        assertEquals(1, contexts.size());
        assertEquals("file", contexts.get(0).fetch("name").orElseThrow());
        assertEquals("hello-world.txt", contexts.get(0).fetch("filename").orElseThrow());

        assertEquals(1, values.size());
        assertEquals(values.get(0), "Hello, World!");
    }
}
