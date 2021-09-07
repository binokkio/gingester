package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ToBytes implements Transformer<String, byte[]> {

    private final Charset charset;

    public ToBytes(Parameters parameters) {
        charset = switchCharsetParameter(parameters.charset);
    }

    private Charset switchCharsetParameter(String charsetParameter) {
        switch (charsetParameter) {
            case "ISO‑8859‑1": return StandardCharsets.ISO_8859_1;
            case "US-ASCII": return StandardCharsets.US_ASCII;
            case "UTF-16": return StandardCharsets.UTF_16;
            case "UTF-16BE": return StandardCharsets.UTF_16BE;
            case "UTF-16LE": return StandardCharsets.UTF_16LE;
            case "UTF-8": return StandardCharsets.UTF_8;
            default: throw new IllegalArgumentException("No case for " + charsetParameter);
        }
    }

    @Override
    public void transform(Context context, String in, Receiver<byte[]> out) throws Exception {
        out.accept(context, in.getBytes(charset));
    }

    public static class Parameters {
        public String charset = "UTF-8";
    }
}
