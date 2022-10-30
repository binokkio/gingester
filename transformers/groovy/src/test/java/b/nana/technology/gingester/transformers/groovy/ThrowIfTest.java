package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ThrowIfTest {

    @Test
    void test() {

        AtomicReference<String> zero = new AtomicReference<>();
        AtomicReference<String> one = new AtomicReference<>();

        new FlowBuilder().cli("" +
                        "-e ExceptionHandler " +
                        "-t Repeat " +
                        "-t StringDef 'Hello, World ${description}!' " +
                        "-t ThrowIf 'Message contains \"1\"' 'in.contains(\"1\")' " +
                        "-- " +
                        "-t ExceptionHandler:Eval 'in.getMessage()'")
                .addTo(zero::set, "ThrowIf")
                .addTo(one::set, "ExceptionHandler")
                .run();

        assertEquals("Hello, World 0!", zero.get());
        assertEquals("Message contains \"1\"", one.get());
    }
}