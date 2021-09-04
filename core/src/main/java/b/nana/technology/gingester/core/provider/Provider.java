package b.nana.technology.gingester.core.provider;

import b.nana.technology.gingester.core.Transformer;

import java.util.Collection;

public interface Provider {
    Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses();
}
