package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TemplatingTest {

    @Test
    public void testMapGetsSerializedToJsonWhenUsedInStringDef() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -ss hello world
                -ss bye world
                -t FetchMap hello bye
                -s map
                -t StringDef '${map}'
                """)
                .add(result::set)
                .run();

        assertEquals("{\"hello\":\"world\",\"bye\":\"world\"}", result.get());
    }

    @Test
    public void testUrlEncoding() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t StringDef 'Hello, World!'
                -s helloWorld
                -t StringDef '${helloWorld?url}'
                """)
                .add(result::set)
                .run();

        assertEquals("Hello%2C%20World!", result.get());
    }
}
