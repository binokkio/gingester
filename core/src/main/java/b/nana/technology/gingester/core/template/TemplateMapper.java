package b.nana.technology.gingester.core.template;

import b.nana.technology.gingester.core.controller.Context;

import java.util.function.Function;

/**
 * Context template mapper.
 * <p>
 * Render strings using the Apache FreeMarker template engine and the Gingester Context as its data model. Template
 * variables are resolved as if they were interpreted by {@link Context#fetch(String...)}. The rendered string is mapped
 * to T by the given mapper Function.
 * <p>
 * If the template is invariant the mapper function is only called once for an instance of TemplateMapper and the result
 * is then returned for every call to render.
 */
public class TemplateMapper<T> {

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

            invariant = render != null ?
                    mapper.apply(render) :
                    null;

        } else if (templateParameters.invariant) {
            invariant = mapper.apply(templateWrapper.render());
        } else {
            invariant = null;
        }
    }

    /**
     * Render this template.
     *
     * If the template is invariant a pre-made T is returned, otherwise the template is rendered with the given context
     * and mapped to T by the mapper Function given during construction.
     *
     * @param context the context to use for rendering this template if rendering is necessary
     * @return the resulting T
     */
    public T render(Context context) {
        return invariant != null ? invariant : mapper.apply(templateWrapper.render(context));
    }
}
