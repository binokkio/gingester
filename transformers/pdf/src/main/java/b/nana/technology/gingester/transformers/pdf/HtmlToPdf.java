package b.nana.technology.gingester.transformers.pdf;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Names(1)
public class HtmlToPdf implements Transformer<String, OutputStreamWrapper> {

    @Override
    public void transform(Context context, String in, Receiver<OutputStreamWrapper> out) throws Exception {
        try (OutputStreamWrapper output = new OutputStreamWrapper()) {
            out.accept(context, output);
            ITextRenderer renderer = new ITextRenderer();
            SharedContext sharedContext = renderer.getSharedContext();
            sharedContext.setPrint(true);
            sharedContext.setInteractive(false);
            renderer.setDocumentFromString(in);
            renderer.layout();
            renderer.createPDF(output);
        }
    }
}
