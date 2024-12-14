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

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.Key;
import java.security.SecureRandom;

@Experimental
@Names(1)
public final class Encrypt implements Transformer<InputStream, InputStream> {

    private final SecureRandom secureRandom = new SecureRandom();
    private final FetchKey fetchKey;
    private final TransformationSpec transformationSpec;
    private final boolean piv;  // Prepend Initialization Vector

    public Encrypt(Parameters parameters) {
        fetchKey = parameters.key;
        transformationSpec = new TransformationSpec(parameters.transformation);
        piv = parameters.piv;
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {

        Key key = (Key) context.require(fetchKey);

        if (key instanceof SecretKey) {

            key = new SecretKeySpec(key.getEncoded(), transformationSpec.getAlgorithm()
                    .orElse("AES"));

            Cipher cipher = Cipher.getInstance(transformationSpec.getTransformation()
                    .orElse("AES/CBC/PKCS5Padding"));

            if (cipher.getAlgorithm().equals("AES") || cipher.getAlgorithm().contains("ECB")) {

                cipher.init(Cipher.ENCRYPT_MODE, key);
                CipherInputStream cipherInputStream = new CipherInputStream(in, cipher);
                out.accept(context, cipherInputStream);

            } else {

                byte[] iv = new byte[16];
                secureRandom.nextBytes(iv);

                cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
                InputStream result =  new CipherInputStream(in, cipher);
                if (piv) result = new SequenceInputStream(new ByteArrayInputStream(iv), result);
                out.accept(context.stash("iv", iv), result);
            }

        } else {
            Cipher cipher = Cipher.getInstance(transformationSpec.getTransformation().orElse(key.getAlgorithm()));
            cipher.init(Cipher.ENCRYPT_MODE, key);
            CipherInputStream cipherInputStream = new CipherInputStream(in, cipher);
            out.accept(context, cipherInputStream);
        }
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"key", "transformation"})
    public static class Parameters {
        public FetchKey key;
        public String transformation;
        public boolean piv;
    }
}
