package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Names(1)
public final class Gcli implements Transformer<Object, Object> {

    private final TemplateParameters gcliTemplateParameters;

    public Gcli(Parameters parameters) {
        gcliTemplateParameters = parameters.gcli;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        new FlowBuilder()
                .seed(context, in)
                .cli(Context.newTemplate(gcliTemplateParameters).render(context, in))
                .add(o -> out.accept(context, o))
                .run();
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "gcli" })
    public static class Parameters {
        public TemplateParameters gcli;
    }
}
