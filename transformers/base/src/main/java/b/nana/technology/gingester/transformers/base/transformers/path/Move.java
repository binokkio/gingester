package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Move implements Transformer<Path, Path> {

    private final Template pathTemplate;
    private final boolean mkdirs;

    public Move(Parameters parameters) {
        pathTemplate = Context.newTemplate(parameters.path);
        mkdirs = parameters.mkdirs;
    }

    @Override
    public void transform(Context context, Path in, Receiver<Path> out) throws Exception {
        
        Path target = Path.of(pathTemplate.render(context));

        if (mkdirs) {
            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        }

        Files.move(in, target);

        out.accept(
                context.stash(Map.of(
                        "description", target,
                        "path", Map.of(
                                "tail", target.getFileName(),
                                "absolute", target.toAbsolutePath()
                        )
                )),
                target
        );
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, path -> o("path", path));
            }
        }

        public TemplateParameters path;
        public boolean mkdirs = true;
    }
}
