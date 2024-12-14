package b.nana.technology.gingester.transformers.crypto;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;
import java.util.List;

public final class CryptoProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                Decrypt.class,
                Encrypt.class,
                GenKeyPair.class,
                KeyToPem.class,
                Pbkdf2.class,
                PemToKeyPair.class
        );
    }
}
