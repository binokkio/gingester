package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

public class Search implements Transformer<Object, Path> {

    private final Path root;
    private final String[] globs;
    private final boolean findDirs;
    private final int maxDepth;

    public Search(Parameters parameters) {
        root = Paths.get(parameters.root).toAbsolutePath();
        globs = parameters.globs;
        findDirs = parameters.findDirs;
        maxDepth = calculateMaxDepth(globs);
    }

    private static int calculateMaxDepth(String[] globs) {
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
        Files.walkFileTree(root, new Visitor(context, out));
    }

    private class Visitor implements FileVisitor<Path> {

        private final Context context;
        private final PathMatcher[] pathMatchers;
        private final Receiver<Path> out;

        public Visitor(Context context, Receiver<Path> out) {
            this.context = context;
            this.out = out;
            this.pathMatchers = createPathMatchers();
        }

        private PathMatcher[] createPathMatchers() {
            PathMatcher[] pathMatchers = new PathMatcher[globs.length];
            for (int i = 0; i < globs.length; i++) {
                pathMatchers[i] = FileSystems.getDefault().getPathMatcher("glob:" + globs[i]);
            }
            return pathMatchers;
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
            return relative.getNameCount() > maxDepth ? SKIP_SUBTREE : CONTINUE;
            // TODO this can be done better
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
        public String[] globs = new String[] { "**/*" };
        public boolean findDirs;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String glob) {
            this.globs = new String[] { glob };
        }
    }
}
