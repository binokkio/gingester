package b.nana.technology.gingester.core.template;

import b.nana.technology.gingester.core.controller.Context;

/**
 * Context template.
 * <p>
 * Render strings using the Apache FreeMarker template engine and the Gingester Context as its data model. Template
 * variables are resolved as if they were interpreted by {@link Context#fetch(String...)}.
 */
public final class Template extends TemplateMapper<String> {

    public Template(TemplateParameters templateParameters) {
        super(templateParameters, s -> s);
    }
}
