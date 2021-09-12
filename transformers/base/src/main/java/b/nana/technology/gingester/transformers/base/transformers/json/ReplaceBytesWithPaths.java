package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

public class ReplaceBytesWithPaths implements Transformer<JsonNode, JsonNode> {

    private final Path directory;
    private final Pattern filenameReplacePattern;
    private final String extension;
    private final OpenOption[] openOptions;
    private final Path pathRelativeTo;

    public ReplaceBytesWithPaths(Parameters parameters) {

        directory = Paths.get(parameters.directory);
        filenameReplacePattern = Pattern.compile(parameters.filenameReplacePattern);
        extension = parameters.extension;
        openOptions = parameters.openOptions;
        pathRelativeTo = Paths.get(parameters.pathRelativeTo != null ? parameters.pathRelativeTo : parameters.directory);

        if (!directory.startsWith(pathRelativeTo)) {
            throw new IllegalStateException("!directory.startsWith(relativeTo)");
        }

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws Exception {
        transform(in, "", new HashSet<>());
        out.accept(context, in);
    }

    private void transform(JsonNode jsonNode, String jsonPointer, Set<Path> usedPaths) throws IOException {
        if (jsonNode.isObject()) {
            Iterator<String> keys = jsonNode.fieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                String childJsonPointer = jsonPointer + "/" + key;
                JsonNode child = jsonNode.get(key);
                if (child.isBinary()) {
                    Path path = findUnusedPath(childJsonPointer, usedPaths);
                    Files.write(path, ((BinaryNode) child).binaryValue(), openOptions);
                    ((ObjectNode) jsonNode).put(key, pathRelativeTo.relativize(path).toString());
                } else if (child.isContainerNode()) {
                    transform(child, childJsonPointer, usedPaths);
                }
            }
        } else if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                String childJsonPointer = jsonPointer + "/" + i;
                JsonNode child = jsonNode.get(i);
                if (child.isBinary()) {
                    Path path = findUnusedPath(childJsonPointer, usedPaths);
                    Files.write(path, ((BinaryNode) child).binaryValue(), openOptions);
                    ((ArrayNode) jsonNode).set(i, ((ArrayNode) jsonNode).textNode(pathRelativeTo.relativize(path).toString()));
                } else if (child.isContainerNode()) {
                    transform(child, childJsonPointer, usedPaths);
                }
            }
        }
    }

    private Path findUnusedPath(String jsonPointer, Set<Path> usedPaths) {
        String filename = filenameReplacePattern.matcher(jsonPointer.substring(1)).replaceAll("_");
        Path path = directory.resolve(filename + extension);
        for (int i = 1; usedPaths.contains(path); i++) {
            path = directory.resolve(filename + '-' + i + extension);
        }
        usedPaths.add(path);
        return path;
    }

    public static class Parameters {
        public String directory = "";
        public String filenameReplacePattern = "[\\\\/|\"'.,:;#*?!<>{}\\s\\p{Cc}]";
        public String extension = "";
        public StandardOpenOption[] openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE_NEW };
        public String pathRelativeTo;
    }
}
