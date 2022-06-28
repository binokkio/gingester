package b.nana.technology.gingester.transformers.base.transformers.bytes;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;

import java.io.ByteArrayOutputStream;

@Pure
public final class FromOutputStream implements Transformer<OutputStreamWrapper, byte[]> {

    @Override
    public void transform(Context context, OutputStreamWrapper in, Receiver<byte[]> out) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        in.wrap(bytes);
        in.onClose(() -> out.accept(context, bytes.toByteArray()));
    }
}
