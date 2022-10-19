package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.lang.reflect.Method;

public final class Transformers {

    private Transformers() {
        throw new UnsupportedOperationException();
    }

    public static boolean isSyncAware(Class<? extends Transformer> transformerClass) {
        try {

            Method prepare = transformerClass.getMethod("prepare", Context.class, Receiver.class);
            Method finish = transformerClass.getMethod("finish", Context.class, Receiver.class);

            boolean isPrepareOverridden = !prepare.getDeclaringClass().equals(Transformer.class);
            boolean isFinishOverridden = !finish.getDeclaringClass().equals(Transformer.class);

            return isPrepareOverridden || isFinishOverridden;

        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
