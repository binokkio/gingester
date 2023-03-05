package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Names(1)
public final class Gcli implements Transformer<Object, Object> {

    private final List<Template> gcliTemplates;

    public Gcli(Parameters parameters) {
        gcliTemplates = parameters.stream().map(Context::newTemplate).collect(Collectors.toList());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        FlowBuilder flowBuilder = new FlowBuilder().seed(context, in);
        gcliTemplates.stream().map(t -> t.render(context, in)).forEach(flowBuilder::cli);
        flowBuilder.add(o -> out.accept(context, o)).run();
    }

    public static class Parameters extends ArrayList<TemplateParameters> {

    }
}
