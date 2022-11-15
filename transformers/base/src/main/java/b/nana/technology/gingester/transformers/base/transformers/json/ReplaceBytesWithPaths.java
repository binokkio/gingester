package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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

@Example(example = "'/tmp/assets'", description = "Write byte arrays to files in /tmp/assets and put the resulting paths in the json")
@Example(example = "'{directory: \"/tmp/assets\", pathsRelativeTo: \"/tmp\"}'", description = "Same as above but with paths relative to /tmp")
public final class ReplaceBytesWithPaths implements Transformer<JsonNode, JsonNode> {

    private final TemplateMapper<Path> directoryTemplate;
    private final TemplateMapper<Path> pathsRelativeToTemplate;
    private final Pattern filenameReplacePattern;
    private final String extension;
    private final OpenOption[] openOptions;

    public ReplaceBytesWithPaths(Parameters parameters) {
        directoryTemplate = Context.newTemplateMapper(parameters.directory, Paths::get);
        pathsRelativeToTemplate = parameters.pathsRelativeTo != null ? Context.newTemplateMapper(parameters.pathsRelativeTo, Paths::get) : null;
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
        Path directory = directoryTemplate.render(context, jsonNode);
        Path pathRelativeTo = pathsRelativeToTemplate != null ? pathsRelativeToTemplate.render(context, jsonNode) : null;
        if (jsonNode.isObject()) {
            Iterator<String> keys = jsonNode.fieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                String childJsonPointer = jsonPointer + "/" + key;
                JsonNode child = jsonNode.get(key);
                if (child.isBinary()) {
                    Path path = findUnusedPath(directory, childJsonPointer, usedPaths);
                    Files.write(path, ((BinaryNode) child).binaryValue(), openOptions);
                    ((ObjectNode) jsonNode).put(key, getReplacement(path, pathRelativeTo));
                } else if (child.isContainerNode()) {
                    transform(context, child, childJsonPointer, usedPaths);
                }
            }
        } else if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                String childJsonPointer = jsonPointer + "/" + i;
                JsonNode child = jsonNode.get(i);
                if (child.isBinary()) {
                    Path path = findUnusedPath(directory, childJsonPointer, usedPaths);
                    Files.write(path, ((BinaryNode) child).binaryValue(), openOptions);
                    ((ArrayNode) jsonNode).set(i, ((ArrayNode) jsonNode).textNode(getReplacement(path, pathRelativeTo)));
                } else if (child.isContainerNode()) {
                    transform(context, child, childJsonPointer, usedPaths);
                }
            }
        }
    }

    private Path findUnusedPath(Path directory, String jsonPointer, Set<Path> usedPaths) throws IOException {
        String filename = filenameReplacePattern.matcher(jsonPointer.substring(1)).replaceAll("_");
        Files.createDirectories(directory);
        Path path = directory.resolve(filename + extension);
        for (int i = 1; usedPaths.contains(path); i++) {
            path = directory.resolve(filename + '-' + i + extension);
        }
        usedPaths.add(path);
        return path;
    }

    private String getReplacement(Path path, Path pathRelativeTo) {
        if (pathRelativeTo != null) {
            return pathRelativeTo.relativize(path).toString();
        } else {
            return path.toString();
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, text -> o("directory", text));
                rule(json -> json.has("template"), json -> o("directory", json));
            }
        }

        public TemplateParameters directory = new TemplateParameters("", true);
        public TemplateParameters pathsRelativeTo;
        public String filenameReplacePattern = "[\\\\/|\"'.,:;#*?!<>\\[\\]{}\\s\\p{Cc}]";
        public String extension = "";
        public StandardOpenOption[] openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE_NEW };
    }
}
