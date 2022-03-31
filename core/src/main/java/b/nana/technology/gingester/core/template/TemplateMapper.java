package b.nana.technology.gingester.core.template;

import b.nana.technology.gingester.core.controller.Context;

import java.util.function.Function;

// TODO javadoc and make Template and extension of TemplateMapper to remove duplicate code
public final class TemplateMapper<T> {

    private final FreemarkerTemplateWrapper templateWrapper;
    private final Function<String, T> mapper;
    private final T invariant;

    public TemplateMapper(TemplateParameters templateParameters, Function<String, T> mapper) {
        this.templateWrapper = FreemarkerTemplateFactory.createTemplate(templateParameters.getTemplateString());
        this.mapper = mapper;

        if (templateParameters.invariant == null) {
            // try to render the template without a data model, if that does not throw then
            // assume the template is invariant
            String render;
            try {
                render = templateWrapper.render();
            } catch (Exception e) {
                render = null;
            }
            invariant = mapper.apply(render);
        } else if (templateParameters.invariant) {
            invariant = mapper.apply(templateWrapper.render());
        } else {
            invariant = null;
        }
    }

    public T render(Context context) {
        return invariant != null ? invariant : mapper.apply(templateWrapper.render(context));
    }
}
