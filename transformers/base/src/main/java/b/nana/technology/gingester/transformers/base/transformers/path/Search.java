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

public class Search implements Transformer<Object, Path> {

    private final Path root;
    private final String[] globs;

    public Search(Parameters parameters) {
        root = Paths.get(parameters.root).toAbsolutePath();
        globs = parameters.globs;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Path> out) throws Exception {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + globs[0]);  // TODO don't ignore the other globs
        Files.walkFileTree(root, new Visitor(context, pathMatcher, out));
    }

    private class Visitor implements FileVisitor<Path> {

        private final Context context;
        private final PathMatcher pathMatcher;
        private final Receiver<Path> out;

        public Visitor(Context context, PathMatcher pathMatcher, Receiver<Path> out) {
            this.context = context;
            this.pathMatcher = pathMatcher;
            this.out = out;
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
            // TODO log? throw?
            return CONTINUE;
        }

        private void handle(Path path) {

            Path relative = root.relativize(path);

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
            }
        }
    }

    public static class Parameters {

        public String root = "";
        public String[] globs = new String[] { "**/*" };

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String glob) {
            this.globs = new String[] { glob };
        }
    }
}
