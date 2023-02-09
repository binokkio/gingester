package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class Def implements Transformer<Object, Path> {

    private final TemplateMapper<Path> trustedTemplate;
    private final TemplateMapper<Path> untrustedTemplate;

    public Def(Parameters parameters) {
        trustedTemplate = Context.newTemplateMapper(parameters.trusted, Paths::get);
        untrustedTemplate = parameters.untrusted == null ? null : Context.newTemplateMapper(parameters.untrusted, Paths::get);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Path> out) {

        Path path = trustedTemplate.render(context, in);
        if (untrustedTemplate != null) {
            Path untrusted = untrustedTemplate.render(context, in);
            String normalized = untrusted.normalize().toString();
            if (normalized.startsWith("/") || normalized.startsWith("..")) {
                throw new IllegalArgumentException(String.format(
                        "%s wanders too far",  // TODO
                        untrusted
                ));
            }
            path = path.resolve(untrusted);
        }

        Path fileName = path.getFileName();
        if (fileName != null) {
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
        } else {
            out.accept(
                    context.stash(Map.of(
                            "description", path,
                            "path", Map.of(
                                    "absolute", path.toAbsolutePath()
                            )
                    )),
                    path
            );
        }
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "trusted", "untrusted" })
    public static class Parameters  {
        public TemplateParameters trusted;
        public TemplateParameters untrusted;
    }
}
