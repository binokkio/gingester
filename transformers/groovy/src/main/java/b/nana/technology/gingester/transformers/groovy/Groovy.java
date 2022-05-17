package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

@Names(1)
public final class Groovy implements Transformer<Object, Object> {

    private final ThreadLocal<TemplateMapper<ScriptWithYield>> scripts;

    public Groovy(Parameters parameters) {
        scripts = ThreadLocal.withInitial(() -> Context.newTemplateMapper(
                parameters.script,
                script -> new ScriptWithYield(new GroovyShell().parse(script))
        ));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        ScriptWithYield script = scripts.get().render(context);
        script.context = context;
        script.out = out;

        Binding binding = new Binding();
        binding.setProperty("context", context);
        binding.setProperty("in", in);
        binding.setProperty("out", out);
        binding.setProperty("yield", script.yield);

        script.script.setBinding(binding);
        script.script.run();
        script.script.setBinding(null);
        script.context = null;
        script.out = null;
    }

    private final static class ScriptWithYield {

        private final Script script;
        private final Closure<Void> yield = new Closure<>(this) {
            @Override
            public Void call(Object... args) {
                out.accept(context, args[0]);
                return null;
            }
        };

        private Context context;
        private Receiver<Object> out;

        private ScriptWithYield(Script script) {
            this.script = script;
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
