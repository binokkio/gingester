package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.smtp.mimetree.MimeTreeNode;

import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GetInlinePlainText implements Transformer<MimeTreeNode, String> {

    private static final Pattern CHARSET_PATTERN = Pattern.compile("charset=\"(.*?)\"");

    @Override
    public void transform(Context context, MimeTreeNode in, Receiver<String> out) {

        StringBuilder inlinePlainText = new StringBuilder();

        in.walk(node -> {

            boolean isAttachment = node.getHeaderValues("Content-Disposition")
                    .anyMatch(v -> v.startsWith("attachment"));

            if (isAttachment)
                return false;

            Optional<String> optionalContentType = node.getHeaderValues("Content-Type")
                    .reduce((a, b) -> { throw new IllegalStateException("Multiple Content-Type headers"); })
                    .filter(s -> s.startsWith("text/plain"));

            if (optionalContentType.isPresent()) {
                String contentType = optionalContentType.get();
                Matcher charsetMatcher = CHARSET_PATTERN.matcher(contentType);
                String charset;
                if (charsetMatcher.find()) {
                    charset = charsetMatcher.group(1);
                } else {
                    charset = "UTF-8";
                }
                try {
                    inlinePlainText.append(new String(node.getBody(), charset));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);  // TODO
                }
            }

            return true;
        });

        out.accept(context, inlinePlainText.toString());
    }
}
