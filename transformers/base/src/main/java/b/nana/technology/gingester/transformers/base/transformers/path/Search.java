package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

public class Search extends Transformer<Void, Path> {

    private final Path root;
    private final String[] globs;

    public Search(Parameters parameters) {
        super(parameters);
        root = Paths.get(parameters.root);
        globs = parameters.globs;
    }

    @Override
    protected void setup(Setup setup) {
        // TODO check if we have 1 or globs.length outputs and emit accordingly
    }

    @Override
    protected void transform(Context context, Void input) throws IOException {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + globs[0]);
        Files.walkFileTree(root, new Visitor(context, pathMatcher));
    }

    private class Visitor implements FileVisitor<Path> {

        private final Context context;
        private final PathMatcher pathMatcher;

        public Visitor(Context context, PathMatcher pathMatcher) {
            this.context = context;
            this.pathMatcher = pathMatcher;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
            handle(path);
            return CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
            handle(path);
            return CONTINUE;  // TODO check path and glob and decide to CONTINUE or SKIP_SUBTREE
        }

        @Override
        public FileVisitResult postVisitDirectory(Path path, IOException e) {
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path path, IOException e) {
            // TODO log
            return CONTINUE;
        }

        private void handle(Path path) {

            if (pathMatcher.matches(path)) {

                Context.Builder contextBuilder = context.extend(Search.this)
                        .description(root.relativize(path).toString());

                emit(contextBuilder, path);
            }
        }
    }

    public static class Parameters {
        public String root;
        public String[] globs = new String[] { "**/*" };
    }
}
