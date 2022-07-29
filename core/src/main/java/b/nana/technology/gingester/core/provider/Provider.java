package b.nana.technology.gingester.core.provider;

import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface Provider {

    Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses();

    default Map<String, String> getCaseHints() {
        return Collections.emptyMap();
    }
}
