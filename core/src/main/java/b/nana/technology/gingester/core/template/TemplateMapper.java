package b.nana.technology.gingester.core.template;

import b.nana.technology.gingester.core.controller.Context;

import java.util.Optional;

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
    private final Mapper<T> mapper;
    private final T invariant;

    public TemplateMapper(TemplateParameters templateParameters, Mapper<T> mapper) {
        this.templateWrapper = templateParameters.createTemplateWrapper();
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
                    map(render) :
                    null;

        } else if (templateParameters.invariant) {
            invariant = map(templateWrapper.render());
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
        return invariant != null ? invariant : map(templateWrapper.render(context));
    }

    /**
     * Check if this template is invariant.
     *
     * An invariant template is rendered and mapped only once during construction the same resulting T is returned for
     * every call to {@link #render(Context)}.
     *
     * @return true if this template is invariant, false otherwise
     */
    public boolean isInvariant() {
        return invariant != null;
    }

    /**
     * Get the invariant T of this template if this template is invariant.
     *
     * @return the invariant T, or empty if this template is not invariant
     */
    public Optional<T> getInvariant() {
        return Optional.ofNullable(invariant);
    }

    /**
     * Get the invariant T of this template.
     *
     * @throws IllegalStateException if this template is not invariant
     * @return the invariant T
     */
    public T requireInvariant() throws IllegalStateException {
        return getInvariant()
                .orElseThrow(() -> new IllegalStateException("getInvariant called a template that is not invariant"));
    }

    private T map(String string) {
        try {
            return mapper.map(string);
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO
        }
    }

    public interface Mapper<T> {
        T map(String string) throws Exception;
    }
}
