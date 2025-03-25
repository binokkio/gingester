package b.nana.technology.gingester.transformers.claude;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GcliExtractorTest {

    @Test
    void test() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder()
                .seedValue("[{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"Can you make the final output uppercase?\"}]},{\"role\":\"assistant\",\"content\":[{\"type\":\"tool_use\",\"id\":\"1\",\"name\":\"str_replace_editor\",\"input\":{\"command\":\"view\",\"path\":\"/main.gcli\"}}]},{\"role\":\"user\",\"content\":[{\"type\":\"tool_result\",\"tool_use_id\":\"1\",\"content\":\"-t StringDef 'Hello, World!' -t StdOut\"}]},{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"I'll modify the GCLI script to make the final output uppercase. To do this, I'll add the `Uppercase` transformer which converts strings to uppercase before they're sent to StdOut.\"},{\"type\":\"tool_use\",\"id\":\"toolu_01PgD1zMJP5GbEeU9aKZhXLy\",\"name\":\"str_replace_editor\",\"input\":{\"command\":\"str_replace\",\"path\":\"/main.gcli\",\"old_str\":\"-t StringDef 'Hello, World!' -t StdOut\",\"new_str\":\"-t StringDef 'Hello, World!' -t Uppercase -t StdOut\"}}]},{\"role\":\"user\",\"content\":[{\"type\":\"tool_result\",\"tool_use_id\":\"toolu_01PgD1zMJP5GbEeU9aKZhXLy\",\"content\":\"-t StringDef 'Hello, World!' -t Uppercase -t StdOut\"}]},{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"I've updated the GCLI script to make the final output uppercase. The modified pipeline now:\\n\\n1. Creates a string with the `StringDef` transformer\\n2. Converts the string to uppercase with the `Uppercase` transformer\\n3. Outputs the uppercase string to standard output with the `StdOut` transformer\\n\\nThis will display \\\"HELLO, WORLD!\\\" instead of \\\"Hello, World!\\\" in the output.\"}]}]")
                .cli("-t ClaudeGcliExtractor")
                .add(result::set)
                .run();

        assertEquals("-t StringDef 'Hello, World!' -t Uppercase -t StdOut", result.get());
    }
}