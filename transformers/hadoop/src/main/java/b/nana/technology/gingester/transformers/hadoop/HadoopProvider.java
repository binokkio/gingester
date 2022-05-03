package b.nana.technology.gingester.transformers.hadoop;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.hadoop.transformers.hdfs.Search;

import java.util.Collection;
import java.util.Set;

public class HadoopProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return Set.of(
                Search.class
        );
    }
}
