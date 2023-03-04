package b.nana.technology.gingester.transformers.base.transformers.xml;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

@Pure
public final class FromInputStream implements Transformer<InputStream, Document> {

    private final ThreadLocal<DocumentBuilder> documentBuilders = ThreadLocal.withInitial(() -> {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException();
        }
    });

    @Override
    public void transform(Context context, InputStream in, Receiver<Document> out) throws IOException, SAXException {
        out.accept(context, documentBuilders.get().parse(in));
    }
}
