package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Move implements Transformer<Path, Path> {

    private final Template pathTemplate;
    private final boolean mkdirs;
    private final CollisionStrategy collisionStrategy;

    public Move(Parameters parameters) {
        pathTemplate = Context.newTemplate(parameters.path);
        mkdirs = parameters.mkdirs;
        collisionStrategy = parameters.collisionStrategy;
    }

    @Override
    public void transform(Context context, Path in, Receiver<Path> out) throws Exception {
        
        Path target = Path.of(pathTemplate.render(context, in));

        if (mkdirs) {
            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        }

        switch (collisionStrategy) {

            case NONE:
                Files.move(in, target);
                break;

            case COUNTER:
                Path given = target;
                int collisions = 0;
                for (;;) {
                    try {
                        Files.move(in, target);
                        break;
                    } catch (FileAlreadyExistsException e) {
                        target = given.resolveSibling(getCounterSibling(given, ++collisions));
                    }
                }
                break;
        }

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

    private String getCounterSibling(Path path, int counter) {

        String filename = path.getFileName().toString();
        String[] parts = filename.split("\\.", 2);

        StringBuilder result = new StringBuilder();
        result
                .append(parts[0])
                .append('-')
                .append(counter);

        if (parts.length > 1)
            result
                    .append('.')
                    .append(parts[1]);

        return result.toString();
    }

    public enum CollisionStrategy {
        NONE,
        COUNTER
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
        public CollisionStrategy collisionStrategy = CollisionStrategy.NONE;
    }
}
