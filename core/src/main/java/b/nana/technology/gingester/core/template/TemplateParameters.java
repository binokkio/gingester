package b.nana.technology.gingester.core.template;

import java.util.Map;

public final class TemplateParameters {

    public String template;
    public TemplateType is = TemplateType.STRING;
    public Boolean invariant;
    public Map<String, Object> kwargs = Map.of();

    public TemplateParameters() {

    }

    public TemplateParameters(String template) {
        this.template = template;
    }

    public TemplateParameters(String template, boolean invariant) {
        this.template = template;
        this.invariant = invariant;
    }

    public TemplateParameters(String template, TemplateType is) {
        this.template = template;
        this.is = is;
    }

    public TemplateParameters(String template, TemplateType is, boolean invariant) {
        this.template = template;
        this.is = is;
        this.invariant = invariant;
    }
}
