package b.nana.technology.gingester.transformers.crypto;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Experimental;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.security.*;
import java.util.Map;

@Experimental
@Names(1)
@Example(example = "", description = "Generate an RSA keypair")
public final class GenKeyPair implements Transformer<Object, KeyPair> {

    private final String algorithm;
    private final int size;

    public GenKeyPair(Parameters parameters) {
        algorithm = parameters.algorithm;
        size = parameters.size;
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.of(Map.of(
                "privk", PrivateKey.class,
                "pubk", PublicKey.class
        ));
    }

    @Override
    public void transform(Context context, Object in, Receiver<KeyPair> out) throws NoSuchAlgorithmException {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
        keyPairGenerator.initialize(size);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        out.accept(context.stash(Map.of(
                "privk", keyPair.getPrivate(),
                "pubk", keyPair.getPublic()
        )), keyPair);
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"algorithm", "size"})
    public static class Parameters {
        public String algorithm = "RSA";
        public int size = 2048;
    }
}
