package b.nana.technology.gingester.transformers.poi;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.poi.xls.ToCsv;

import java.util.Collection;
import java.util.List;

public final class PoiProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                b.nana.technology.gingester.transformers.poi.xls.ToCsv.class,
                b.nana.technology.gingester.transformers.poi.xlsx.ToCsv.class
        );
    }
}
