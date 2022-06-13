package b.nana.technology.gingester.transformers.base.transformers.hash;

import b.nana.technology.gingester.core.annotations.Names;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Names(1)
public final class Sha1 extends Hash {

    public Sha1(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("sha-1");
    }
}
