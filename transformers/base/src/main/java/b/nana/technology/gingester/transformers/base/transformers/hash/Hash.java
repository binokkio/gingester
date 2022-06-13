package b.nana.technology.gingester.transformers.base.transformers.hash;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

abstract class Hash implements Transformer<InputStream, byte[]> {

    private final byte[] buffer;

    public Hash(Parameters parameters) {
        buffer = new byte[parameters.bufferSize];
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<byte[]> out) throws Exception {
        MessageDigest messageDigest = getMessageDigest();
        DigestInputStream digestInputStream = new DigestInputStream(in, messageDigest);
        while (digestInputStream.read(buffer) != -1);
        out.accept(context, messageDigest.digest());
    }

    protected abstract MessageDigest getMessageDigest() throws NoSuchAlgorithmException;

    public static class Parameters {

        public int bufferSize = 8192;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int bufferSize) {
            this.bufferSize = bufferSize;
        }
    }
}
