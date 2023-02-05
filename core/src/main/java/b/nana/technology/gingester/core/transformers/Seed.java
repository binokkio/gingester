package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

public final class Seed implements Transformer<Object, Object> {

    private Class<?> outputType = Object.class;

    public void setOutputType(Class<?> outputType) {
        this.outputType = outputType;
    }

    @Override
    public Object getOutputType() {
        return outputType;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        out.accept(context, in);
    }
}
