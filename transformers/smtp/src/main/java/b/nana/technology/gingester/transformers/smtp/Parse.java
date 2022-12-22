package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.smtp.mimetree.MimeTreeNode;

import java.io.InputStream;

public final class Parse implements Transformer<InputStream, MimeTreeNode> {

    @Override
    public void transform(Context context, InputStream in, Receiver<MimeTreeNode> out) throws Exception {
        out.accept(context, MimeTreeNode.parse(in));
    }
}
