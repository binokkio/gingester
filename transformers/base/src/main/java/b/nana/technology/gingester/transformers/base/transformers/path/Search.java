package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import static java.nio.file.FileVisitResult.CONTINUE;

public class Search extends Transformer<Void, Path> {

    private final Path root;
    private final String[] globs;
    private final boolean stash;

    public Search(Parameters parameters) {
        super(parameters);
        root = Paths.get(parameters.root).toAbsolutePath();
        globs = parameters.globs;
        stash = parameters.stash;
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

            Path relative = root.relativize(path);

            if (pathMatcher.matches(relative)) {

                Context.Builder contextBuilder = context.extend(Search.this)
                        .description(relative.toString());

                if (stash) {
                    contextBuilder.stash(Map.of(
                            "path", Map.of(
                                    "tail", path.getFileName(),
                                    "relative", relative,
                                    "full", path
                            )
                    ));
                }

                emit(contextBuilder, path);
            }
        }
    }

    public static class Parameters {

        public String root = "";
        public String[] globs = new String[] { "**/*" };
        public boolean stash = true;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String glob) {
            this.globs = new String[] { glob };
        }
    }
}
