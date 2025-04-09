package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

@Names(1)
public final class GcliToGraphTxt implements Transformer<String, String> {

    @Override
    public void transform(Context context, String in, Receiver<String> out) throws Exception {
        out.accept(context, new FlowBuilder().cli(in).toTxtGraph());
    }
}
