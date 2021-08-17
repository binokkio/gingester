package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestReplaceBytesWithPaths {

    @Test
    void testMinimalParameters() throws IOException {

        JsonNode result = testWithParameters(new ReplaceBytesWithPaths.Parameters());

        assertEquals("world", result.get("hello").textValue());
        assertEquals("bin1", result.get("bin1").textValue());
        assertEquals("bin2", result.get("bin2").textValue());
        assertEquals("nested_bin3", result.get("nested").get("bin3").textValue());
        assertEquals("nested_bin4_", result.get("nested").get("bin4?").textValue());
        assertEquals("nested_bin4_-1", result.get("nested").get("bin4!").textValue());
        assertEquals("world", result.get("bye").textValue());

        Files.delete(Paths.get("bin1"));
        Files.delete(Paths.get("bin2"));
        Files.delete(Paths.get("nested_bin3"));
        Files.delete(Paths.get("nested_bin4_"));
        Files.delete(Paths.get("nested_bin4_-1"));
    }

    @Test
    void testAllParameters() throws IOException {

        Path tempDirectory = Files.createTempDirectory("gingester-");

        ReplaceBytesWithPaths.Parameters parameters = new ReplaceBytesWithPaths.Parameters();
        parameters.directory = tempDirectory.resolve("assets/blobs").toString();
        parameters.filenameReplacePattern = "[/]";
        parameters.extension = ".bin";
        parameters.openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE };
        parameters.pathRelativeTo = tempDirectory.resolve("assets").toString();

        JsonNode result = testWithParameters(parameters);

        assertEquals("world", result.get("hello").textValue());
        assertEquals("blobs/bin1.bin", result.get("bin1").textValue());
        assertEquals("blobs/bin2.bin", result.get("bin2").textValue());
        assertEquals("blobs/nested_bin3.bin", result.get("nested").get("bin3").textValue());
        assertEquals("blobs/nested_bin4?.bin", result.get("nested").get("bin4?").textValue());
        assertEquals("blobs/nested_bin4!.bin", result.get("nested").get("bin4!").textValue());
        assertEquals("world", result.get("bye").textValue());

        Files.delete(tempDirectory.resolve("assets/blobs/bin1.bin"));
        Files.delete(tempDirectory.resolve("assets/blobs/bin2.bin"));
        Files.delete(tempDirectory.resolve("assets/blobs/nested_bin3.bin"));
        Files.delete(tempDirectory.resolve("assets/blobs/nested_bin4?.bin"));
        Files.delete(tempDirectory.resolve("assets/blobs/nested_bin4!.bin"));
        Files.delete(tempDirectory.resolve("assets/blobs/"));
        Files.delete(tempDirectory.resolve("assets/"));
        Files.delete(tempDirectory);
    }

    private JsonNode testWithParameters(ReplaceBytesWithPaths.Parameters parameters) {

        ReplaceBytesWithPaths replaceBytesWithPaths = new ReplaceBytesWithPaths(parameters);

        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.put("hello", "world");
        input.put("bin1", "Hello, World!".getBytes());
        input.put("bin2", "Bye, World!".getBytes());
        input.put("bye", "world");

        ObjectNode nested = JsonNodeFactory.instance.objectNode();
        input.set("nested", nested);
        nested.put("bin3", "Hello, nested World!".getBytes());
        nested.put("bin4?", "Hello, nested collision World!".getBytes());
        nested.put("bin4!", "Hello, nested collision World!".getBytes());

        AtomicReference<JsonNode> result = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(replaceBytesWithPaths, input);
        gBuilder.link(replaceBytesWithPaths, result::set);
        gBuilder.build().run();

        return result.get();
    }
}
