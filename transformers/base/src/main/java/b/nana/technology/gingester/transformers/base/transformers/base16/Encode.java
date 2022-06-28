package b.nana.technology.gingester.transformers.base.transformers.base16;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.nio.charset.StandardCharsets;

public final class Encode implements Transformer<byte[], String> {

    private static final byte[] LOWER = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    @Override
    public void transform(Context context, byte[] in, Receiver<String> out) {
        byte[] result = new byte[in.length * 2];
        for (int i = 0; i < in.length; i++) {
            int v = in[i] & 0xFF;
            result[i * 2] = LOWER[v >>> 4];
            result[i * 2 + 1] = LOWER[v & 0x0F];
        }
        out.accept(context, new String(result, StandardCharsets.UTF_8));
    }
}
