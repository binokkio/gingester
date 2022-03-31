package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Example(example = "'[\"*.csv\", \".txt\"]'", description = "Find all files with names ending on \".csv\" or \".txt\" in the working directory")
@Example(example = "'{globs: \"*\", findDirs: true}'", description = "Find all files and directories in the working directory")
@Example(example = "'{root: \"/tmp\", globs: \"*\"}'", description = "Find all files in the /tmp directory")
public class Search implements Transformer<Object, Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Search.class);

    private final FileSystem fileSystem = FileSystems.getDefault();

    private final Template rootTemplate;
    private final List<Template> globTemplates;
    private final List<Template> patternTemplates;
    private final boolean findDirs;

    public Search(Parameters parameters) {
        rootTemplate = Context.newTemplate(parameters.root);
        globTemplates = parameters.globs.stream().map(Context::newTemplate).collect(Collectors.toList());
        patternTemplates = parameters.patterns.stream().map(Context::newTemplate).collect(Collectors.toList());
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
        List<String> patterns = patternTemplates.stream().map(t -> t.render(context)).collect(Collectors.toList());
        Files.walkFileTree(root, new Visitor(root, globs, patterns, context, out));
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

        public Visitor(Path root, List<String> globs, List<String> patterns, Context context, Receiver<Path> out) {
            this.root = root;
            this.pathMatchers = Stream.concat(
                    globs.stream().map(s -> "glob:" + s).map(fileSystem::getPathMatcher),
                    patterns.stream().map(s -> "regex:" + s).map(fileSystem::getPathMatcher)
            ).collect(Collectors.toList());
            this.maxDepth = calculateMaxDepth(globs, patterns);
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

    private static int calculateMaxDepth(List<String> globs, List<String> patterns) {
        if (patterns.isEmpty()) {
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

    public static class Parameters {

        public TemplateParameters root = new TemplateParameters("");
        public List<TemplateParameters> globs = Collections.singletonList(new TemplateParameters("**"));
        public List<TemplateParameters> patterns = Collections.emptyList();
        public boolean findDirs;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(@JsonProperty("globs") TemplateParameters glob) {
            this.globs = Collections.singletonList(glob);
        }
    }
}
