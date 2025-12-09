package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.annotations.Stashes;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Names(1)
@Passthrough
@Stashes(stash = "description", type = Integer.class)
public final class Repeat implements Transformer<Object, Object> {

    private final TemplateMapper<Integer> times;

    public Repeat(Parameters parameters) {
        times = new TemplateMapper<>(parameters.times, Integer::parseInt);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws InterruptedException {
        for (int i = 0; i < times.render(context, in); i++) {
            if (Thread.interrupted()) throw new InterruptedException();
            out.accept(context.stash("description", i), in);
        }
    }

    public static class Parameters {

        public TemplateParameters times = new TemplateParameters("2");

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int times) {
            this.times = new TemplateParameters(Integer.toString(times));
        }

        @JsonCreator
        public Parameters(String times) {
            this.times = new TemplateParameters(times);
        }
    }
}
