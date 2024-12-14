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
import java.io.InputStream;
import java.security.Key;

import static b.nana.technology.gingester.core.common.Util.readExactly;

@Experimental
@Names(1)
public final class Decrypt implements Transformer<InputStream, InputStream> {

    private final FetchKey fetchKey;
    private final FetchKey fetchIv;

    public Decrypt(Parameters parameters) {
        fetchKey = parameters.key;
        fetchIv = parameters.iv;
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {

        Key key = (Key) context.require(fetchKey);

        if (key instanceof SecretKey) {

            key = new SecretKeySpec(key.getEncoded(), "AES");

            byte[] iv;
            if (fetchIv != null) iv = (byte[]) context.require(fetchIv);
            else iv = readExactly(in, 16);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            CipherInputStream cipherInputStream = new CipherInputStream(in, cipher);
            out.accept(context.stash("iv", iv), cipherInputStream);
        } else {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            CipherInputStream cipherInputStream = new CipherInputStream(in, cipher);
            out.accept(context, cipherInputStream);
        }
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"key"})
    public static class Parameters {
        public FetchKey key;
        public FetchKey iv;
    }
}
