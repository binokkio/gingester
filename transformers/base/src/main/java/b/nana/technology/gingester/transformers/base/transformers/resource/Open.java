package b.nana.technology.gingester.transformers.base.transformers.resource;

import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Open implements Transformer<Object, InputStream> {

    private final TemplateMapper<Path> trustedTemplate;
    private final TemplateMapper<Path> untrustedTemplate;
    private final boolean optional;

    public Open(Parameters parameters) {
        trustedTemplate = Context.newTemplateMapper(parameters.trusted, Paths::get);
        untrustedTemplate = parameters.untrusted == null ? null : Context.newTemplateMapper(parameters.untrusted, Paths::get);
        optional = parameters.optional;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void transform(Context context, Object in, Receiver<InputStream> out) throws Exception {

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

        String resourcePath = path.toString();
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream != null) out.accept(context.stash("description", resourcePath), inputStream);
            else if (!optional) throw new NullPointerException("getResourceAsStream(\"" + resourcePath + "\") returned null");
        }
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "trusted", "untrusted" })
    public static class Parameters  {
        public TemplateParameters trusted;
        public TemplateParameters untrusted;
        public boolean optional = false;
    }
}
