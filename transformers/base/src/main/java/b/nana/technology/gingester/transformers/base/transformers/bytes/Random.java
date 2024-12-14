package b.nana.technology.gingester.transformers.base.transformers.bytes;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.security.SecureRandom;

@Example(example = "16", description = "Create a byte array with 16 random bytes")
public final class Random implements Transformer<Object, byte[]> {

    private final int length;
    private final java.util.Random random;

    public Random(Parameters parameters) {
        length = parameters.length;
        random = parameters.secure ? new SecureRandom() : new java.util.Random();
    }

    @Override
    public void transform(Context context, Object in, Receiver<byte[]> out) throws Exception {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        out.accept(context, bytes);
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"length"})
    public static class Parameters {
        public int length;
        public boolean secure = true;
    }
}
