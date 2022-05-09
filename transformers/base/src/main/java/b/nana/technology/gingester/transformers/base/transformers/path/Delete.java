package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public final class Delete implements Transformer<Path, Path> {

    private final boolean recursive;

    public Delete(Parameters parameters) {
        recursive = parameters.recursive;
    }

    @Override
    public void transform(Context context, Path in, Receiver<Path> out) throws Exception {

        if (recursive) {
            Files.walkFileTree(in, new FileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.delete(in);
        }

        out.accept(context, in);
    }

    public static class Parameters {

        public boolean recursive;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(boolean recursive) {
            this.recursive = recursive;
        }
    }
}
