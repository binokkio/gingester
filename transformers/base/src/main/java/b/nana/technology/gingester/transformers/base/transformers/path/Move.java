package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Move implements Transformer<Path, Path> {

    private final Context.Template pathTemplate;
    private final boolean mkdirs;

    public Move(Parameters parameters) {
        pathTemplate = Context.newTemplate(parameters.path);
        mkdirs = parameters.mkdirs;
    }

    @Override
    public void transform(Context context, Path in, Receiver<Path> out) throws Exception {
        
        Path target = Path.of(pathTemplate.render(context));

        Path parent = target.getParent();
        if (mkdirs && parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        Files.move(in, target);
        out.accept(
                context.stash(Map.of(
                        "description", target.toString(),
                        "path", Map.of(
                                "full", target.toAbsolutePath()
                        )
                )),
                target
        );
    }

    public static class Parameters {

        public String path;
        public boolean mkdirs = true;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String path) {
            this.path = path;
        }
    }
}
