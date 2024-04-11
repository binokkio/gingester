package b.nana.technology.gingester.transformers.base.transformers.base16;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class Decode implements Transformer<String, byte[]> {

    @Override
    public void transform(Context context, String in, Receiver<byte[]> out) {
        out.accept(context, HexFormat.of().parseHex(in));
    }
}
