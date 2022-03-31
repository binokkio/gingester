package b.nana.technology.gingester.core.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public final class TemplateParameters {

    public String template;
    public Interpretation is = Interpretation.STRING;
    public Boolean invariant;
    // TODO add a syntax enum with the different Freemarker options and "OFF"/"NONE"

    @JsonCreator
    public TemplateParameters() {

    }

    @JsonCreator
    public TemplateParameters(String template) {
        this.template = template;
    }

    public TemplateParameters(String template, boolean invariant) {
        this.template = template;
        this.invariant = invariant;
    }

    public TemplateParameters(String template, boolean invariant, Interpretation is) {
        this.template = template;
        this.invariant = invariant;
        this.is = is;
    }

    @JsonIgnore
    public String getTemplateString() {

        switch (is) {

            case FILE: return readTemplateFile(template).orElseThrow();
            case RESOURCE: return readTemplateResource(template).orElseThrow();
            case STRING: return template;

            default:
                throw new IllegalStateException("No case for " + is);
        }
    }

    @JsonIgnore
    FreemarkerTemplateWrapper createTemplateWrapper() {
        return FreemarkerTemplateFactory.createTemplate(getTemplateString());
    }

    public enum Interpretation {
        FILE,
        RESOURCE,
        STRING
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
        InputStream inputStream = TemplateParameters.class.getResourceAsStream(template);
        if (inputStream == null) return Optional.empty();
        try {
            return Optional.of(new String(inputStream.readAllBytes()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read template resource", e);
        }
    }
}
