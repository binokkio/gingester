package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

@Description("Search the filesystem for paths")
@Example(example = "'*'", description = "Find all files in the working directory")
@Example(example = "'**'", description = "Find all files in the working directory and subdirectories recursively")
@Example(example = "'*.csv'", description = "Find all files with names ending on \".csv\" in the working directory")
@Example(example = "'[\"*.csv\", \"*.txt\"]'", description = "Find all files with names ending on \".csv\" or \".txt\" in the working directory")
@Example(example = "'{globs: \"*\", findDirs: true}'", description = "Find all files and directories in the working directory")
@Example(example = "'{globs: \"*\", root: \"/tmp\"}'", description = "Find all files in the /tmp directory")
@Example(example = "'{regexes: \"h[ae]l{2,}o\"}'", description = "Find files matching the given regular expression")
@Example(example = "'/absolute/path'", description = "Will NOT work! Set the `root` parameter to the deepest directory containing the target paths instead", test = false)
public class Search implements Transformer<Object, Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Search.class);

    private final FileSystem fileSystem = FileSystems.getDefault();

    private final Template rootTemplate;
    private final List<Template> globTemplates;
    private final List<Template> regexTemplates;
    private final boolean findDirs;

    public Search(Parameters parameters) {
        rootTemplate = Context.newTemplate(parameters.root);
        globTemplates = parameters.globs.stream().map(Context::newTemplate).collect(Collectors.toList());
        regexTemplates = parameters.regexes.stream().map(Context::newTemplate).collect(Collectors.toList());
        findDirs = parameters.findDirs;

        for (TemplateParameters globTemplateParameters : parameters.globs) {
            String glob = globTemplateParameters.getTemplateString();
            if (glob.startsWith("/") || glob.startsWith(".") || glob.contains("//") || glob.contains("/./") || glob.contains("/../")) {
                LOGGER.warn("Glob '{}' will not match anything, use '{root:\"\",globs:\"\"}' to search outside of the working directory and use globs without \".\", \"..\", \"/\" as path elements", glob);
            }
        }
    }

    @Override
    public final void transform(Context context, Object in, Receiver<Path> out) throws Exception {
        Path root = Path.of(rootTemplate.render(context)).toAbsolutePath();
        List<String> globs = globTemplates.stream().map(t -> t.render(context)).collect(Collectors.toList());
        List<String> regexes = regexTemplates.stream().map(t -> t.render(context)).collect(Collectors.toList());
        Files.walkFileTree(root, new Visitor(root, globs, regexes, context, out));
    }

    /**
     * Enrich the PathSearch stash.
     *
     * Subclasses can implement this method to add information to the stash by mutating the
     * map passed to the `stash` parameter.
     *
     * @param relative the path matching the PathSearch globs relative to the PathSearch root
     * @param stash the stash to enrich
     */
    protected void enrich(Path relative, Map<String, Object> stash) {

    }

    private class Visitor implements FileVisitor<Path> {

        private final Path root;
        private final List<PathMatcher> pathMatchers;
        private final int maxDepth;
        private final Context context;
        private final Receiver<Path> out;

        public Visitor(Path root, List<String> globs, List<String> regexes, Context context, Receiver<Path> out) {
            this.root = root;
            this.pathMatchers = Stream.concat(
                    globs.stream().map(s -> "glob:" + s).map(fileSystem::getPathMatcher),
                    regexes.stream().map(s -> "regex:" + s).map(fileSystem::getPathMatcher)
            ).collect(Collectors.toList());
            this.maxDepth = calculateMaxDepth(globs, regexes);
            this.context = context;
            this.out = out;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
            Path relative = root.relativize(path);
            handle(path, relative);
            return CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
            Path relative = root.relativize(path);
            if (findDirs) handle(path, relative);
            return relative.getNameCount() > maxDepth ? SKIP_SUBTREE : CONTINUE;  // TODO this can be done better
        }

        @Override
        public FileVisitResult postVisitDirectory(Path path, IOException e) {
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path path, IOException e) {
            return CONTINUE;  // TODO log? throw?
        }

        private void handle(Path path, Path relative) {
            for (PathMatcher pathMatcher : pathMatchers) {
                if (pathMatcher.matches(relative)) {
                    Map<String, Object> stash = new HashMap<>();
                    stash.put("description", relative.toString());
                    Map<String, Object> pathStash = new HashMap<>();
                    stash.put("path", pathStash);
                    pathStash.put("tail", path.getFileName());
                    pathStash.put("relative", relative);
                    pathStash.put("absolute", path);
                    enrich(relative, stash);
                    out.accept(context.stash(stash), path);
                    return;
                }
            }
        }
    }

    private static int calculateMaxDepth(List<String> globs, List<String> regexes) {
        if (regexes.isEmpty()) {
            int maxDepth = 0;
            for (String glob : globs) {
                if (glob.contains("**")) {
                    maxDepth = Integer.MAX_VALUE;
                } else {
                    maxDepth = Math.max(maxDepth, glob.split("/").length);
                }
            }
            return maxDepth;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, text -> o("globs", text));
                rule(JsonNode::isArray, array -> o("globs", array));
                rule(json -> json.has("template"), json -> o("globs", json));
            }
        }

        public TemplateParameters root = new TemplateParameters("", true);
        public List<TemplateParameters> globs = Collections.emptyList();
        public List<TemplateParameters> regexes = Collections.emptyList();
        public boolean findDirs;
    }
}
