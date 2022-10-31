package b.nana.technology.gingester.transformers.protobuf;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;
import java.util.Set;

public final class ProtobufProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return Set.of(
                ToJson.class,
                ToJsonString.class
        );
    }
}
