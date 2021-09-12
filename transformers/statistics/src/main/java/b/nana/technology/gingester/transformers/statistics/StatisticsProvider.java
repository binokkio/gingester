package b.nana.technology.gingester.transformers.statistics;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;
import java.util.List;

public class StatisticsProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                b.nana.technology.gingester.transformers.statistics.transformers.json.Statistics.class,
                b.nana.technology.gingester.transformers.statistics.transformers.string.Statistics.class
        );
    }
}
