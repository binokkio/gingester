package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Link implements Transformer<Path, Path> {

    private final Template pathTemplate;
    private final boolean mkdirs;

    public Link(Parameters parameters) {
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

        Files.createLink(target, in);
        out.accept(
                context.stash(Map.of(
                        "description", target,
                        "path", Map.of(
                                "absolute", target.toAbsolutePath(),
                                "tail", target.getFileName()
                        )
                )),
                target
        );
    }

    public static class Parameters {

        public TemplateParameters path;
        public boolean mkdirs = true;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters path) {
            this.path = path;
        }
    }
}
