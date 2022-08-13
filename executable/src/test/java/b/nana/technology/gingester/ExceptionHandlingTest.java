package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
                .attach((c, i) -> exceptionHandlerIsFlawless.add(c.isFlawless()), "ExceptionHandler")
                .attach((c, i) -> exceptionHandlerDownstreamIsFlawless.add(c.isFlawless()), "ExceptionHandlerDownstream")
                .attach((c, i) -> oneIsFlawless.add(c.isFlawless()), "One")
                .attach((c, i) -> twoIsFlawless.add(c.isFlawless()), "Two")
                .attach((c, i) -> threeIsFlawless.add(c.isFlawless()), "Three")
                .run();

        assertEquals(List.of(true), exceptionHandlerIsFlawless);
        assertEquals(List.of(true), exceptionHandlerDownstreamIsFlawless);
        assertEquals(List.of(true, true, true), oneIsFlawless);
        assertEquals(List.of(true, false, true), twoIsFlawless);
        assertEquals(List.of(true, false, true), threeIsFlawless);

    }
}
