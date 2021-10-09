package b.nana.technology.gingester.core.provider;

import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;

public interface Provider {
    Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses();
}
