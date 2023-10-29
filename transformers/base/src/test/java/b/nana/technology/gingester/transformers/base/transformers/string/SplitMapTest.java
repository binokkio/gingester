package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.Node;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SplitMapTest {

    @Test
    void testSimple() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef one,two,three " +
                "-t StringSplitMap , a b c")
                .add(result::set)
                .run();

        assertEquals("one", result.get().get("a"));
        assertEquals("two", result.get().get("b"));
        assertEquals("three", result.get().get("c"));
    }

    @Test
    void testComplex1() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef ,one,two,,three, " +
                "-t StringSplitMap , a b c d e f")
                .add(result::set)
                .run();

        assertEquals("", result.get().get("a"));
        assertEquals("one", result.get().get("b"));
        assertEquals("two", result.get().get("c"));
        assertEquals("", result.get().get("d"));
        assertEquals("three", result.get().get("e"));
        assertEquals("", result.get().get("f"));
    }

    @Test
    void testComplex2() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef , " +
                "-t StringSplitMap , a b")
                .add(result::set)
                .run();

        assertEquals("", result.get().get("a"));
        assertEquals("", result.get().get("b"));
    }

    @Test
    void testComplex3() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef ,, " +
                "-t StringSplitMap , a b c")
                .add(result::set)
                .run();

        assertEquals("", result.get().get("a"));
        assertEquals("", result.get().get("b"));
    }

    @Test
    void testAccumulator1() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!,foo,bar' " +
                "-t StringSplitMap , a! b c")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get().get("a"));
        assertEquals("foo", result.get().get("b"));
        assertEquals("bar", result.get().get("c"));
    }

    @Test
    void testAccumulator2() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'foo,Hello, World!,bar' " +
                "-t StringSplitMap , a b! c")
                .add(result::set)
                .run();

        assertEquals("foo", result.get().get("a"));
        assertEquals("Hello, World!", result.get().get("b"));
        assertEquals("bar", result.get().get("c"));
    }

    @Test
    void testAccumulator3() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'foo,bar,Hello, World!' " +
                "-t StringSplitMap , a b c!")
                .add(result::set)
                .run();

        assertEquals("foo", result.get().get("a"));
        assertEquals("bar", result.get().get("b"));
        assertEquals("Hello, World!", result.get().get("c"));
    }

    @Test
    void testAccumulator4() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'foo,bar,Hello, World!' " +
                "-t StringSplitMap , a b c")
                .add(result::set)
                .run();

        assertEquals("foo", result.get().get("a"));
        assertEquals("bar", result.get().get("b"));
        assertEquals("Hello, World!", result.get().get("c"));
    }

    @Test
    void testAccumulator5() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'foo,bar' " +
                "-t StringSplitMap , a b c " +
                "-e ErrorHandler -- " +
                "-pt ErrorHandler ")
                .add(result::set)
                .run();

        assertEquals("Split produced less values than keys", result.get().getMessage());
    }

    @Test
    void testAccumulator6() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'foo,bar,baz' " +
                "-t StringSplitMap , a b c! d e " +
                "-e ErrorHandler -- " +
                "-pt ErrorHandler ")
                .add(result::set)
                .run();

        assertEquals("Split produced less values than keys", result.get().getMessage());
    }
}