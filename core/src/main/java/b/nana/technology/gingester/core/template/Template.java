package b.nana.technology.gingester.core.template;

import b.nana.technology.gingester.core.controller.Context;

/**
 * Context template.
 * <p>
 * Render strings using the Apache FreeMarker template engine and the Gingester Context as its data model. Template
 * variables are resolved as if they were interpreted by {@link Context#fetch(String...)}.
 */
public final class Template {

    /*
    We could have an alternative/extension TemplateMapper<T> class which takes a Function<String, T> as
    constructor parameter and if the template is invariant uses it to create a ~`final T invariantMapping`
    and always returns that through the render methods.
     */

    private final FreemarkerTemplateWrapper templateWrapper;
    private final String invariant;

    public Template(TemplateParameters templateParameters) {
        templateWrapper = FreemarkerTemplateFactory.createTemplate(templateParameters.getTemplateString());
        if (templateParameters.invariant == null) {
            String render;
            try {
                render = templateWrapper.render();
            } catch (Exception e) {
                render = null;
            }
            invariant = render;
        } else if (templateParameters.invariant) {
            invariant = templateWrapper.render();
        } else {
            invariant = null;
        }
    }

    /**
     * Render this context template with a data model.
     *
     * @return the rendered template
     */
    public String render(Context context) {
        return invariant != null ? invariant : templateWrapper.render(context);
    }
}
