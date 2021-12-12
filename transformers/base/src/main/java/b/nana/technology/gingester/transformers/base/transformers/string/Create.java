package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Create implements Transformer<Object, String> {

    private final Context.Template template;

    public Create(Parameters parameters) {
        template = Context.newTemplate(getTemplateString(parameters.template, parameters.interpretation));
    }

    private String getTemplateString(String template, TemplateParameterInterpretation interpretation) {

        switch (interpretation) {

            case FILE: return readTemplateFile(template).orElseThrow();
            case RESOURCE: return readTemplateResource(template).orElseThrow();
            case TEMPLATE: return template;

            case AUTO:
                return Stream.of(
                                () -> readTemplateResource(template),
                                (Supplier<Optional<String>>) () -> readTemplateFile(template))
                        .map(Supplier::get)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst().orElse(template);

            default:
                throw new IllegalStateException("No case for " + interpretation);
        }
    }

    @Override
    public void transform(Context context, Object in, Receiver<String> out) throws InterruptedException {
        out.accept(context, template.render(context));
    }

    public static class Parameters {

        public String template;
        public TemplateParameterInterpretation interpretation = TemplateParameterInterpretation.AUTO;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String template) {
            this.template = template;
        }
    }

    public enum TemplateParameterInterpretation {
        AUTO,
        FILE,
        RESOURCE,
        TEMPLATE
    }

    private static Optional<String> readTemplateFile(String template) {
        Path path = Paths.get(template);
        if (!Files.exists(path)) return Optional.empty();
        try {
            return Optional.of(Files.readString(Paths.get(template)));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read template file", e);
        }
    }

    private static Optional<String> readTemplateResource(String template) {
        InputStream inputStream = Create.class.getResourceAsStream(template);
        if (inputStream == null) return Optional.empty();
        try {
            return Optional.of(new String(inputStream.readAllBytes()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read template resource", e);
        }
    }
}
