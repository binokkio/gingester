package b.nana.technology.gingester.transformers.crypto;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Experimental;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

@Experimental
@Names(1)
@Example(example = "", description = "Encode private or public key input and yield PEM")
public final class KeyToPem implements Transformer<Key, String> {

    @Override
    public void transform(Context context, Key in, Receiver<String> out) {

        String algorithm = in.getAlgorithm().toUpperCase();

        if (in instanceof PrivateKey) {

            out.accept(context,
                    "-----BEGIN " + algorithm + " PRIVATE KEY-----\n" +
                    Base64.getMimeEncoder().encodeToString(in.getEncoded()) +
                    "\n-----END " + algorithm + " PRIVATE KEY-----\n");

        } else if (in instanceof PublicKey) {

            out.accept(context,
                    "-----BEGIN " + algorithm + " PUBLIC KEY-----\n" +
                    Base64.getMimeEncoder().encodeToString(in.getEncoded()) +
                    "\n-----END " + algorithm + " PUBLIC KEY-----\n");

        } else throw new IllegalArgumentException("Key is neither private nor public");
    }
}
