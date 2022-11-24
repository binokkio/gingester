package b.nana.technology.gingester.test;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.test.transformers.Emphasize;
import b.nana.technology.gingester.test.transformers.Question;
import b.nana.technology.gingester.test.transformers.SyncCounter;

import java.util.Collection;
import java.util.List;

public class TestProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                Emphasize.class,
                Question.class,
                SyncCounter.class,
                b.nana.technology.gingester.test.transformers.a.NameCollision.class,
                b.nana.technology.gingester.test.transformers.b.NameCollision.class
        );
    }
}
