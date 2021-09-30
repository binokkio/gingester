package b.nana.technology.gingester.transformers.jetty;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.jetty.http.*;

import java.util.Collection;
import java.util.List;

public class JettyProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                Keycloak.class,
                Responder.class,
                Server.class,
                Split.class
        );
    }
}
