package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ScopeTest {

    @Test
    void testSimple() {

        Deque<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-cr A:scope-test-simple-nested.cli " +
                "-l Results " +
                "-cr B:scope-test-simple-nested.cli " +
                "-l Results " +
                "-t Results:Passthrough")
                .add(results::add)
                .run();

        assertEquals(4, results.size());
        assertEquals("Hello, World 0!", results.remove());
        assertEquals("Hello, World 1!", results.remove());
        assertEquals("Hello, World 0!", results.remove());
        assertEquals("Hello, World 1!", results.remove());
    }

    @Test
    void testAbsoluteReferenceToParentScope() {

        Deque<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-cr Scope:scope-test-reference-to-parent-scope.cli '{target:\"$Results\"}' " +
                "-t Results:Passthrough")
                .add(results::add)
                .run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!", results.remove());
    }

    @Test
    void testRelativeReferenceToParentScope() {

        Deque<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-cr Scope:scope-test-reference-to-parent-scope.cli '{target:\"..$Results\"}' " +
                "-t Results:Passthrough")
                .add(results::add)
                .run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!", results.remove());
    }

    @Test
    void testLocalTargetTemplating() {

        Deque<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-cr Scope:scope-test-local-target-templating.cli")
                .add(results::add)
                .run();

        assertEquals(2, results.size());
        assertEquals("Hello, World 0!", results.remove());
        assertEquals("Hello, World 1!", results.remove());
    }

    @Test
    void testSyncInScope() {

        Deque<Integer> results = new ConcurrentLinkedDeque<>();

        new FlowBuilder().cli("" +
                "-cr Scope1:scope-test-sync-in-nested.cli {count:1} -l Results " +
                "-cr Scope2:scope-test-sync-in-nested.cli {count:2} -l Results " +
                "-cr Scope3:scope-test-sync-in-nested.cli {count:3} -l Results " +
                "-t Results:Passthrough")
                .add(results::add)
                .run();

        List<Integer> sorted = results.stream().sorted().collect(Collectors.toList());
        assertEquals(3, sorted.size());
        assertEquals(1, sorted.get(0));
        assertEquals(2, sorted.get(1));
        assertEquals(3, sorted.get(2));
    }
}
