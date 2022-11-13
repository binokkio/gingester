package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

public abstract class SimpleScriptTransformer implements Transformer<Object, Object> {

    private final ThreadLocal<TemplateMapper<Script>> scripts;

    public SimpleScriptTransformer(TemplateParameters scriptTemplate) {
        scripts = ThreadLocal.withInitial(() -> Context.newTemplateMapper(
                scriptTemplate,
                script -> new GroovyShell().parse(script)
        ));
    }

    protected final Object getResult(Context context, Object in) {

        Binding binding = new Binding();
        binding.setProperty("context", context);
        binding.setProperty("in", in);

        Script script = scripts.get().render(context, in);
        script.setBinding(binding);
        Object result = script.run();
        script.setBinding(null);

        return result;
    }
}
