package b.nana.technology.gingester.transformers.jetty;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.jetty.http.Keycloak;
import b.nana.technology.gingester.transformers.jetty.http.Respond;
import b.nana.technology.gingester.transformers.jetty.http.Server;
import b.nana.technology.gingester.transformers.jetty.http.Split;

import java.util.Collection;
import java.util.List;

public final class JettyProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                Keycloak.class,
                Respond.class,
                Server.class,
                Split.class
        );
    }
}
