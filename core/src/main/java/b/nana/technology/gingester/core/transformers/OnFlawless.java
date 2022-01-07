package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collections;

@Names(1)
public final class OnFlawless implements Transformer<Object, Object> {

    private static final String FLAWLESS_SIGNAL = "flawless signal";

    @Override
    public void setup(SetupControls controls) {
        controls.syncs(Collections.singletonList("__seed__"));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        if (context.isFlawless()) {
            out.accept(context, FLAWLESS_SIGNAL);
        }
    }
}
