package b.nana.technology.gingester.transformers.jetty;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.jetty.http.*;

import java.util.Collection;
import java.util.List;

public final class JettyProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                BasicAuth.class,
                Keycloak.class,
                Oidc.class,
                Proxy.class,
                Respond.class,
                RespondPath.class,
                Server.class,
                SetHeaders.class,
                SetStatus.class,
                Split.class
        );
    }
}
