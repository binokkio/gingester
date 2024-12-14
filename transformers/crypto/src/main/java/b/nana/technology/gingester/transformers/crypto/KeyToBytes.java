package b.nana.technology.gingester.transformers.crypto;

import b.nana.technology.gingester.core.annotations.Experimental;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.security.Key;

@Experimental
@Names(1)
public final class KeyToBytes implements Transformer<Key, byte[]> {

    @Override
    public void transform(Context context, Key in, Receiver<byte[]> out) {
        out.accept(context, in.getEncoded());
    }
}
