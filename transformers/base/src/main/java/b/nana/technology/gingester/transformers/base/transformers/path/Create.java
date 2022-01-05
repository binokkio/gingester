package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class Create implements Transformer<Object, Path> {

    private final Context.Template pathTemplate;

    public Create(Parameters parameters) {
        pathTemplate = Context.newTemplate(parameters.path);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Path> out) throws Exception {
        Path path = Paths.get(pathTemplate.render(context));
        out.accept(
                context.stash(Map.of(
                        "description", path.toString(),
                        "path", Map.of(
                                "absolute", path.toAbsolutePath(),
                                "tail", path.getFileName()
                        )
                )),
                path
        );
    }

    public static class Parameters {

        public String path;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String path) {
            this.path = path;
        }
    }
}
