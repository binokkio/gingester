package b.nana.technology.gingester.core.template;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import static b.nana.technology.gingester.core.template.FreemarkerTemplateFactory.JACKSON_WRAPPER;

public final class TemplateParameters {

    public String template;
    public Interpretation is = Interpretation.STRING;
    public Boolean invariant;
    public Map<String, Object> kwargs = Map.of();

    // TODO add a syntax enum with the different Freemarker options and "OFF"/"NONE"

    public TemplateParameters() {

    }

    public TemplateParameters(String template) {
        this.template = template;
    }

    public TemplateParameters(String template, boolean invariant) {
        this.template = template;
        this.invariant = invariant;
    }

    public TemplateParameters(String template, Interpretation is) {
        this.template = template;
        this.is = is;
    }

    public TemplateParameters(String template, Interpretation is, boolean invariant) {
        this.template = template;
        this.is = is;
        this.invariant = invariant;
    }

    FreemarkerTemplateWrapper createTemplateWrapper() {
        return FreemarkerTemplateFactory.createTemplate(
                getTemplateName(),
                getTemplateString()
        );
    }

    @JsonIgnore
    public String getTemplateString() {
        return FreemarkerTemplateFactory.createCliTemplate(
                getTemplateName(),
                getRawTemplateString()
        ).render(JACKSON_WRAPPER.wrap(kwargs));
    }

    private String getTemplateName() {

        switch (is) {

            case FILE: return "FILE:" + template;
            case RESOURCE: return "RESOURCE:" + template;
            case STRING: return "STRING";

            default:
                throw new IllegalStateException("No case for " + is);
        }
    }

    private String getRawTemplateString() {

        switch (is) {

            case FILE: return readTemplateFile(template).orElseThrow();
            case RESOURCE: return readTemplateResource(template).orElseThrow();
            case STRING: return template;

            default:
                throw new IllegalStateException("No case for " + is);
        }
    }

    public enum Interpretation {
        FILE,
        RESOURCE,
        STRING
    }

    private static Optional<String> readTemplateFile(String template) {
        Path path = Paths.get(template);
        try {
            return Optional.of(Files.readString(path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read template file", e);
        }
    }

    private static Optional<String> readTemplateResource(String template) {
        try (InputStream inputStream = TemplateParameters.class.getResourceAsStream(template)) {
            if (inputStream == null)
                throw new IllegalArgumentException("Resource not found: " + template);
            return Optional.of(new String(inputStream.readAllBytes()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read template resource", e);
        }
    }
}
