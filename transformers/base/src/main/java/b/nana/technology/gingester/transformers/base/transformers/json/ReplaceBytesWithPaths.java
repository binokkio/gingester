package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
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

public final class ReplaceBytesWithPaths implements Transformer<JsonNode, JsonNode> {

    private final Template directoryTemplate;
    private final Template pathsRelativeToTemplate;
    private final Pattern filenameReplacePattern;
    private final String extension;
    private final OpenOption[] openOptions;

    public ReplaceBytesWithPaths(Parameters parameters) {
        directoryTemplate = Context.newTemplate(parameters.directory);
        pathsRelativeToTemplate = Context.newTemplate(parameters.pathsRelativeTo != null ? parameters.pathsRelativeTo : parameters.directory);
        filenameReplacePattern = Pattern.compile(parameters.filenameReplacePattern);
        extension = parameters.extension;
        openOptions = parameters.openOptions;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws Exception {
        transform(context, in, "", new HashSet<>());
        out.accept(context, in);
    }

    private void transform(Context context, JsonNode jsonNode, String jsonPointer, Set<Path> usedPaths) throws IOException {
        Path pathRelativeTo = Paths.get(pathsRelativeToTemplate.render(Context.newTestContext()));
        if (jsonNode.isObject()) {
            Iterator<String> keys = jsonNode.fieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                String childJsonPointer = jsonPointer + "/" + key;
                JsonNode child = jsonNode.get(key);
                if (child.isBinary()) {
                    Path path = findUnusedPath(context, childJsonPointer, usedPaths);
                    Files.write(path, ((BinaryNode) child).binaryValue(), openOptions);
                    ((ObjectNode) jsonNode).put(key, pathRelativeTo.relativize(path).toString());
                } else if (child.isContainerNode()) {
                    transform(context, child, childJsonPointer, usedPaths);
                }
            }
        } else if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                String childJsonPointer = jsonPointer + "/" + i;
                JsonNode child = jsonNode.get(i);
                if (child.isBinary()) {
                    Path path = findUnusedPath(context, childJsonPointer, usedPaths);
                    Files.write(path, ((BinaryNode) child).binaryValue(), openOptions);
                    ((ArrayNode) jsonNode).set(i, ((ArrayNode) jsonNode).textNode(pathRelativeTo.relativize(path).toString()));
                } else if (child.isContainerNode()) {
                    transform(context, child, childJsonPointer, usedPaths);
                }
            }
        }
    }

    private Path findUnusedPath(Context context, String jsonPointer, Set<Path> usedPaths) throws IOException {
        String filename = filenameReplacePattern.matcher(jsonPointer.substring(1)).replaceAll("_");
        Path directory = Paths.get(directoryTemplate.render(context));
        Files.createDirectories(directory);
        Path path = directory.resolve(filename + extension);
        for (int i = 1; usedPaths.contains(path); i++) {
            path = directory.resolve(filename + '-' + i + extension);
        }
        usedPaths.add(path);
        return path;
    }

    public static class Parameters {
        public TemplateParameters directory = new TemplateParameters("");
        public TemplateParameters pathsRelativeTo;
        public String filenameReplacePattern = "[\\\\/|\"'.,:;#*?!<>\\[\\]{}\\s\\p{Cc}]";
        public String extension = "";
        public StandardOpenOption[] openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE_NEW };
    }
}
