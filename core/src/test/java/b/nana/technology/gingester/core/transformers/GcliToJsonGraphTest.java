package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GcliToJsonGraphTest {

    @Test
    void test() {

        AtomicReference<String> result = new AtomicReference<>();

        String gcli = """
                -t StringDef 'Hello, World!'
                -t StringDef 'Bye, World!'
                """;

        new FlowBuilder()
                .seedValue(gcli)
                .cli("""
                -t GcliToJsonGraph
                """)
                .add(result::set)
                .run();

        assertEquals("{\"graph\":{\"nodes\":{\"StringDef\":{\"label\":\"StringDef\",\"metadata\":{\"transformer\":\"StringDef\",\"parameters\":{\"template\":{\"template\":\"Hello, World!\",\"is\":\"STRING\",\"invariant\":null,\"kwargs\":{}}}}},\"StringDef_1\":{\"label\":\"StringDef\",\"metadata\":{\"transformer\":\"StringDef\",\"parameters\":{\"template\":{\"template\":\"Bye, World!\",\"is\":\"STRING\",\"invariant\":null,\"kwargs\":{}}}}}},\"edges\":[{\"source\":\"StringDef\",\"target\":\"StringDef_1\"}]}}", result.get());
    }
}