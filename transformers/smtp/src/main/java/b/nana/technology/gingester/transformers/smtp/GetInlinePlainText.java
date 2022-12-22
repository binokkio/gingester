package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.smtp.mimetree.MimeTreeNode;

public final class GetInlinePlainText implements Transformer<MimeTreeNode, String> {

    @Override
    public void transform(Context context, MimeTreeNode in, Receiver<String> out) {

        StringBuilder inlinePlainText = new StringBuilder();

        in.walk(node -> {

            boolean isAttachment = node.getHeader("Content-Disposition")
                    .filter(s -> s.startsWith("attachment"))
                    .isPresent();

            if (isAttachment)
                return false;

            boolean isPlainText = node.getHeader("Content-Type")
                    .filter(s -> s.startsWith("text/plain"))
                    .isPresent();

            if (isPlainText)
                inlinePlainText.append(node.decodeBody());

            return true;
        });

        out.accept(context, inlinePlainText.toString());
    }
}
