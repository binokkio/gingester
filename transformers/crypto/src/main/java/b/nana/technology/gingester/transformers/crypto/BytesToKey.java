package b.nana.technology.gingester.transformers.crypto;

import b.nana.technology.gingester.core.annotations.Experimental;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

@Experimental
@Names(1)
public final class BytesToKey implements Transformer<byte[], SecretKey> {

    private final String algorithm;

    public BytesToKey(Parameters parameters) {
        algorithm = parameters.algorithm;
    }

    @Override
    public void transform(Context context, byte[] in, Receiver<SecretKey> out) {
        out.accept(context, new SecretKeySpec(in, algorithm));
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"algorithm"})
    public static class Parameters {
        public String algorithm = "AES";
    }
}
