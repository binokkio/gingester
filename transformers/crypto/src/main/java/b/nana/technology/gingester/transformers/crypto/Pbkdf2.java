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
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

@Experimental
@Names(1)
public final class Pbkdf2 implements Transformer<String, SecretKey> {

    private final FetchKey fetchSalt;
    private final int iterations;
    private final String hashFunction;
    private final int keyLength;

    public Pbkdf2(Parameters parameters) {
        fetchSalt = parameters.salt;
        iterations = parameters.iterations;
        hashFunction = parameters.hashFunction;
        keyLength = parameters.keyLength;
    }

    @Override
    public void transform(Context context, String in, Receiver<SecretKey> out) throws NoSuchAlgorithmException, InvalidKeySpecException {

        // fetch salt
        byte[] salt = (byte[]) context.require(fetchSalt);

        // generate key
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmac" + hashFunction);
        KeySpec keySpec = new PBEKeySpec(in.toCharArray(), salt, iterations, keyLength);
        SecretKey secretKey = factory.generateSecret(keySpec);

        out.accept(context, secretKey);
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"salt", "iterations", "hashFunction", "keyLength"})
    public static class Parameters {
        public FetchKey salt = new FetchKey("salt");
        public int iterations = 10;
        public String hashFunction = "SHA256";
        public int keyLength = 128;
    }
}
