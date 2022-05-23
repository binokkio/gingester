package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

@Names(1)
public final class Filter implements Transformer<Object, Object> {

    private final ThreadLocal<TemplateMapper<Script>> scripts;

    public Filter(Parameters parameters) {
        scripts = ThreadLocal.withInitial(() -> Context.newTemplateMapper(
                parameters.script,
                script -> new GroovyShell().parse(script)
        ));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        Binding binding = new Binding();
        binding.setProperty("context", context);
        binding.setProperty("in", in);

        Script script = scripts.get().render(context);
        script.setBinding(binding);
        Object result = script.run();
        script.setBinding(null);

        try {
            boolean asBoolean = (boolean) result;
            if (asBoolean) {
                out.accept(context, in);
            }
        } catch (ClassCastException e) {
            throw new IllegalStateException("Filter did not return a boolean but returned \"" + result + "\"");
        }
    }

    public static class Parameters {

        public TemplateParameters script;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters script) {
            this.script = script;
        }
    }
}
