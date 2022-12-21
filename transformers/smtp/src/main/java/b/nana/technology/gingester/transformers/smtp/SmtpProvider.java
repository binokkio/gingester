package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;
import java.util.List;

public final class SmtpProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                GetInlinePlainText.class,
                Parse.class,
                Send.class,
                Server.class
        );
    }
}
