package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class Def implements Transformer<Object, Path> {

    private final Template pathTemplate;

    public Def(Parameters parameters) {
        pathTemplate = Context.newTemplate(parameters.path);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Path> out) {
        Path path = Paths.get(pathTemplate.render(context, in));
        out.accept(
                context.stash(Map.of(
                        "description", path,
                        "path", Map.of(
                                "absolute", path.toAbsolutePath(),
                                "tail", path.getFileName()
                        )
                )),
                path
        );
    }

    public static class Parameters {

        public TemplateParameters path;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters path) {
            this.path = path;
        }
    }
}
