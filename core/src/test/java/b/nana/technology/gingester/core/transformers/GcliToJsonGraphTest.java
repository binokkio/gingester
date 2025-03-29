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
                -sfpt
                -ss message 'Hello, World!'
                -stt Merge message
                """;

        new FlowBuilder()
                .seedValue(gcli)
                .cli("-t GcliToJsonGraph")
                .add(result::set)
                .run();

        assertEquals("{\"graph\":{\"nodes\":{\"Passthrough\":{\"label\":\"Passthrough\",\"metadata\":{\"transformer\":\"Passthrough\",\"parameters\":null}},\"StashString\":{\"label\":\"StashString\",\"metadata\":{\"transformer\":\"StashString\",\"parameters\":{\"stash\":\"message\",\"template\":{\"template\":\"Hello, World!\",\"is\":\"STRING\",\"invariant\":null,\"kwargs\":{}}}}},\"Merge\":{\"label\":\"Merge\",\"metadata\":{\"transformer\":\"Merge\",\"parameters\":{\"instructions\":[\"message > message\"]}}}},\"edges\":[{\"source\":\"Passthrough\",\"target\":\"StashString\",\"relation\":\"link\"},{\"source\":\"StashString\",\"target\":\"Merge\",\"relation\":\"link\"},{\"source\":\"Passthrough\",\"target\":\"Merge\",\"relation\":\"sync\"}]}}", result.get());
    }
}