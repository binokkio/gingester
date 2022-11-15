package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.transformers.path.collisionstrategy.CollisionStrategy;
import b.nana.technology.gingester.transformers.base.transformers.path.collisionstrategy.NoCollisionStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class Move implements Transformer<Path, Path> {

    private final TemplateMapper<Path> targetTemplate;
    private final boolean mkdirs;
    private final CollisionStrategy collisionStrategy;

    public Move(Parameters parameters) {
        targetTemplate = Context.newTemplateMapper(parameters.path, Paths::get);
        mkdirs = parameters.mkdirs;
        collisionStrategy = parameters.collisionStrategy;
    }

    @Override
    public void transform(Context context, Path in, Receiver<Path> out) throws Exception {
        
        Path target = targetTemplate.render(context, in);

        if (mkdirs) {
            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        }

        Path result = collisionStrategy.apply(
                context,
                in,
                target,
                targetTemplate,
                path -> Files.move(in, path)
        );

        out.accept(
                context.stash(Map.of(
                        "description", result,
                        "path", Map.of(
                                "tail", result.getFileName(),
                                "absolute", result.toAbsolutePath()
                        )
                )),
                result
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
        public CollisionStrategy collisionStrategy = new NoCollisionStrategy();
    }
}
