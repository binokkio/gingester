package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

public final class Seed implements Transformer<Object, Object> {

    private Object value;

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public Object getOutputType() {
        return value != null ? value.getClass() : Object.class;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        out.accept(context, value == null ? in : value);
    }
}
