package b.nana.technology.gingester.transformers.base.transformers.bytes;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.nio.charset.StandardCharsets;

public class ToString implements Transformer<byte[], String> {

    @Override
    public void transform(Context context, byte[] in, Receiver<String> out) throws Exception {
        out.accept(context, new String(in));
    }
}
