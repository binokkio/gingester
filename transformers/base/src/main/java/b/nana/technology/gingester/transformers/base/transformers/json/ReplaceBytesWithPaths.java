package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.*;
import java.util.Iterator;

public class ReplaceBytesWithPaths extends Transformer<JsonNode, JsonNode> {

    private final Path directory;
    private final String extension;
    private final Path relativeTo;
    private final OpenOption[] openOptions;

    public ReplaceBytesWithPaths(Parameters parameters) {
        super(parameters);

        directory = Paths.get(parameters.directory);
        extension = parameters.extension;
        relativeTo = Paths.get(parameters.relativeTo);
        openOptions = parameters.openOptions;

        if (!directory.startsWith(relativeTo)) {
            throw new IllegalStateException("!directory.startsWith(relativeTo)");
        }

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void transform(Context context, JsonNode input) throws Exception {
        transform(input, "");
        emit(context, input);
    }

    private void transform(JsonNode jsonNode, String jsonPointer) throws IOException {
        if (jsonNode.isObject()) {
            Iterator<String> keys = jsonNode.fieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                String childJsonPointer = jsonPointer + "/" + key;
                JsonNode child = jsonNode.get(key);
                if (child.isBinary()) {
                    Path path = directory.resolve(childJsonPointer.substring(1).replaceAll("[^A-Za-z0-9]", "-") + extension);
                    Files.write(path, ((BinaryNode) child).binaryValue(), openOptions);
                    ((ObjectNode) jsonNode).put(key, relativeTo.relativize(path).toString());
                } else if (child.isContainerNode()) {
                    transform(child, childJsonPointer);
                }
            }
        } else if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                String childJsonPointer = jsonPointer + "/" + i;
                JsonNode child = jsonNode.get(i);
                if (child.isBinary()) {
                    Path path = directory.resolve(childJsonPointer.substring(1).replaceAll("[^A-Za-z0-9]", "-") + extension);
                    Files.write(path, ((BinaryNode) child).binaryValue(), openOptions);
                    ((ArrayNode) jsonNode).set(i, ((ArrayNode) jsonNode).textNode(relativeTo.relativize(path).toString()));
                } else if (child.isContainerNode()) {
                    transform(child, childJsonPointer);
                }
            }
        }
    }

    public static class Parameters {
        public String directory;
        public String extension = "";
        public String relativeTo;
        public StandardOpenOption[] openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE_NEW };
    }
}
