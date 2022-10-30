package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

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
}
