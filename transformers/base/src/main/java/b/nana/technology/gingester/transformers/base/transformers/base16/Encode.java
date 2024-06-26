package b.nana.technology.gingester.transformers.base.transformers.base16;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class Encode implements Transformer<byte[], String> {

    @Override
    public void transform(Context context, byte[] in, Receiver<String> out) {
        out.accept(context, HexFormat.of().formatHex(in));
    }
}
