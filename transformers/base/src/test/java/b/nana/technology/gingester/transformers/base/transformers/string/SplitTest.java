package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SplitTest {

    @Test
    void testSimple() {

        ArrayDeque<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t StringDef one,two,three " +
                "-t StringSplit ,")
                .attach(results::add)
                .run();

        assertEquals(List.of("one", "two", "three"), new ArrayList<>(results));
    }

    @Test
    void testComplex1() {

        ArrayDeque<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t StringDef ,one,two,,three, " +
                "-t StringSplit ,")
                .attach(results::add)
                .run();

        assertEquals(List.of("", "one", "two", "", "three", ""), new ArrayList<>(results));
    }

    @Test
    void testComplex2() {

        ArrayDeque<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t StringDef , " +
                "-t StringSplit ,")
                .attach(results::add)
                .run();

        assertEquals(List.of("", ""), new ArrayList<>(results));
    }

    @Test
    void testComplex3() {

        ArrayDeque<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t StringDef ,, " +
                "-t StringSplit ,")
                .attach(results::add)
                .run();

        assertEquals(List.of("", "", ""), new ArrayList<>(results));
    }
}