package b.nana.technology.gingester.transformers.pdf;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;
import java.util.List;

public final class PdfProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                HtmlToPdf.class
        );
    }
}
