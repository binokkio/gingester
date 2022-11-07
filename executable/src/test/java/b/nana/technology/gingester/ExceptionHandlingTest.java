package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionHandlingTest {

    @Test
    void testIsFlawless() {

        List<Boolean> exceptionHandlerIsFlawless = new ArrayList<>();
        List<Boolean> exceptionHandlerDownstreamIsFlawless = new ArrayList<>();
        List<Boolean> oneIsFlawless = new ArrayList<>();
        List<Boolean> twoIsFlawless = new ArrayList<>();
        List<Boolean> threeIsFlawless = new ArrayList<>();

        new FlowBuilder().cli("" +
                "-t ExceptionHandler:Passthrough " +
                "-t ExceptionHandlerDownstream:Passthrough " +
                "-- " +
                "-t Repeat 3 " +
                "-t One:Passthrough " +
                "-t Two:Passthrough " +
                "-e ExceptionHandler " +
                "-t Three:Passthrough " +
                "-t Monkey")
                .addTo((c, i) -> exceptionHandlerIsFlawless.add(c.isFlawless()), "ExceptionHandler")
                .addTo((c, i) -> exceptionHandlerDownstreamIsFlawless.add(c.isFlawless()), "ExceptionHandlerDownstream")
                .addTo((c, i) -> oneIsFlawless.add(c.isFlawless()), "One")
                .addTo((c, i) -> twoIsFlawless.add(c.isFlawless()), "Two")
                .addTo((c, i) -> threeIsFlawless.add(c.isFlawless()), "Three")
                .run();

        assertEquals(List.of(true), exceptionHandlerIsFlawless);
        assertEquals(List.of(true), exceptionHandlerDownstreamIsFlawless);
        assertEquals(List.of(true, true, true), oneIsFlawless);
        assertEquals(List.of(true, false, true), twoIsFlawless);
        assertEquals(List.of(true, false, true), threeIsFlawless);
    }

    @Test
    void testConvergingAfterExceptionHandling() {

        Deque<String> result = new ArrayDeque<>();

        new FlowBuilder()
                .cli(getClass().getResource("/configurations/converging-after-exception-handling.cli"))
                .add(result::add)
                .run();

        assertEquals(2, result.size());
        assertEquals("hello", result.remove());
        assertEquals("world", result.remove());
    }
}
