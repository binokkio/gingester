package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

public final class Search implements Transformer<Object, Path> {

    private final FileSystem fileSystem = FileSystems.getDefault();

    private final Context.Template rootTemplate;
    private final List<Context.Template> globTemplates;
    private final boolean findDirs;

    public Search(Parameters parameters) {
        rootTemplate = Context.newTemplate(parameters.root);
        globTemplates = parameters.globs.stream().map(Context::newTemplate).collect(Collectors.toList());
        findDirs = parameters.findDirs;
    }

    private static int calculateMaxDepth(List<String> globs) {
        int maxDepth = 0;
        for (String glob : globs) {
            if (glob.contains("**")) {
                maxDepth = Integer.MAX_VALUE;
            } else {
                maxDepth = Math.max(maxDepth, glob.split("/").length);
            }
        }
        return maxDepth;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Path> out) throws Exception {
        Path root = Path.of(rootTemplate.render(context));
        List<String> globs = globTemplates.stream().map(t -> t.render(context)).collect(Collectors.toList());
        Files.walkFileTree(root, new Visitor(root, globs, context, out));
    }

    private class Visitor implements FileVisitor<Path> {

        private final Path root;
        private final List<PathMatcher> pathMatchers;
        private final int maxDepth;
        private final Context context;
        private final Receiver<Path> out;

        public Visitor(Path root, List<String> globs, Context context, Receiver<Path> out) {
            this.root = root;
            this.pathMatchers = globs.stream().map(s -> "glob:" + s).map(fileSystem::getPathMatcher).collect(Collectors.toList());
            this.maxDepth = calculateMaxDepth(globs);
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
                    out.accept(
                            context.stash(Map.of(
                                    "description", relative.toString(),
                                    "path", Map.of(
                                            "tail", path.getFileName(),
                                            "relative", relative,
                                            "full", path
                                    )
                            )),
                            path
                    );
                    return;
                }
            }
        }
    }

    public static class Parameters {

        public String root = "";
        public List<String> globs = Collections.singletonList("**");
        public boolean findDirs;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String glob) {
            this.globs = Collections.singletonList(glob);
        }
    }
}
