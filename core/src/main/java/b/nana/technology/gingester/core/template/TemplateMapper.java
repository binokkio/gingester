package b.nana.technology.gingester.core.template;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

/**
 * Context template mapper.
 * <p>
 * Render strings using the Apache FreeMarker template engine and the Gingester Context as its data model. Template
 * variables are resolved as if they were interpreted by {@link Context#fetch(FetchKey)}. The rendered string is mapped
 * to T by the given mapper Function.
 * <p>
 * If the template is invariant the mapper function is only called once for an instance of TemplateMapper and the result
 * is then returned for every call to render.
 */
public class TemplateMapper<T> {

    private final TemplateType is;
    private final Template sourceTemplate;
    private final FetchKey sourceStash;
    private final Map<String, Object> kwargs;
    private final FreemarkerTemplateWrapper template;
    private final Mapper<T> mapper;
    private final T invariant;

    public TemplateMapper(TemplateParameters parameters, Mapper<T> mapper) {

        if (parameters == null) {
            throw new IllegalArgumentException("Template parameters is null");
        }

        this.kwargs = parameters.kwargs;
        this.mapper = mapper;

        switch (parameters.is) {

            case STRING:
                is = TemplateType.STRING;
                sourceTemplate = null;
                sourceStash = null;
                template = FreemarkerTemplateFactory.createTemplate("STRING", parameters.template);
                invariant = tryInvariant(parameters.invariant);
                break;

            case STASH:
                is = TemplateType.STASH;
                sourceTemplate = null;
                sourceStash = new FetchKey(parameters.template);
                template = null;
                invariant = null;
                break;

            case FILE:
            case HOT_FILE:
            case RESOURCE:
                sourceTemplate = new Template(new TemplateParameters(parameters.template));
                sourceStash = null;
                if (parameters.is != TemplateType.HOT_FILE && sourceTemplate.isInvariant()) {
                    is = TemplateType.STRING;
                    template = createTemplateWrapper(
                            is.name() + ":" + sourceTemplate.requireInvariant(),
                            readSource(sourceTemplate.requireInvariant(), parameters.is),
                            kwargs);
                    invariant = tryInvariant(parameters.invariant);
                } else {
                    is = parameters.is;
                    template = null;
                    invariant = null;
                }
                break;

            default: throw new IllegalStateException("No case for " + parameters.is);
        }
    }

    private FreemarkerTemplateWrapper createTemplateWrapper(String name, String template, Map<String, Object> kwargs) {
        return FreemarkerTemplateFactory.createTemplate(
                name,
                FreemarkerTemplateFactory.createCliTemplate(
                        name,
                        template
                ).render(kwargs));
    }

    private T tryInvariant(Boolean hint) {

        if (hint == null) {

            // try to render the template without a data model, if that does not throw then
            // assume the template is invariant

            String render = null;
            if (!template.getRaw().contains("${") && !template.getRaw().contains("<#")) {
                try {
                    render = template.render();
                } catch (Exception e) {
                    // ignore, render stays null, and we assume this template is not invariant
                }
            }

            return render != null ?
                    map(render) :
                    null;

        } else if (hint) {
            return map(template.render());
        } else {
            return null;
        }
    }

    /**
     * Render this template.
     * <p>
     * If the template is invariant a pre-made T is returned, otherwise the template is rendered and mapped to T
     * by the mapper Function given during construction.
     *
     * @param context the context to use for rendering this template if rendering is necessary
     * @return the resulting T
     */
    public T render(Context context) {
        return render(context, null);
    }

    /**
     * Render this template.
     * <p>
     * If the template is invariant a pre-made T is returned, otherwise the template is rendered and mapped to T
     * by the mapper Function given during construction.
     *
     * @param context the context to use for rendering this template if rendering is necessary
     * @param in the in to use for rendering this template if rendering is necessary
     * @return the resulting T
     */
    public T render(Context context, Object in) {

        if (invariant != null)
            return invariant;

        if (template != null)
            return map(template.render(new ContextPlus(context, in)));

        switch (is) {

            case STASH:
                Object stash = context.require(sourceStash);
                return map(createTemplateWrapper(
                        is.name() + ":" + sourceStash,
                        stash instanceof TextNode ? ((TextNode) stash).textValue() : stash.toString(),
                        kwargs).render(new ContextPlus(context, in)));

            case FILE:
            case HOT_FILE:
            case RESOURCE:
                String source = sourceTemplate.render(context, in);
                return map(createTemplateWrapper(
                        is.name() + ":" + source,
                        readSource(source, is),
                        kwargs).render(new ContextPlus(context, in)));

            default: throw new IllegalStateException("No case for " + is);
        }
    }

    /**
     * Check if this template is invariant.
     * <p>
     * An invariant template is rendered and mapped only once during construction and the same resulting T is
     * returned for every call to {@link #render(Context, Object)}.
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

    private static String readSource(String source, TemplateType templateType) {
        switch (templateType) {
            case FILE: return readTemplateFile(source);
            case RESOURCE: return readTemplateResource(source);
            default: throw new IllegalArgumentException("No case for " + templateType);
        }
    }

    private static String readTemplateFile(String template) {
        Path path = Paths.get(template);
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read template file", e);
        }
    }

    private static String readTemplateResource(String template) {
        try (InputStream inputStream = TemplateParameters.class.getResourceAsStream(template)) {
            if (inputStream == null)
                throw new IllegalArgumentException("Resource not found: " + template);
            return new String(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read template resource", e);
        }
    }

    public interface Mapper<T> {
        T map(String string) throws Exception;
    }
}
