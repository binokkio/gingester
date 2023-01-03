package b.nana.technology.gingester.transformers.smtp.mimetree;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeUtility;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.Field;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MimeTreeNode {

    private static final Pattern CHARSET_PATTERN = Pattern.compile("charset=[\"']?([^ \"']+)");

    public static MimeTreeNode parse(byte[] bytes) throws MimeException, IOException {
        return parse(new ByteArrayInputStream(bytes));
    }

    public static MimeTreeNode parse(InputStream inputStream) throws MimeException, IOException {
        MimeTreeBuilder mimeTreeBuilder = new MimeTreeBuilder();
        MimeStreamParser mimeStreamParser = new MimeStreamParser();
        mimeStreamParser.setContentHandler(mimeTreeBuilder);
        mimeStreamParser.parse(inputStream);
        return mimeTreeBuilder.getRoot();
    }

    final String type;
    final MimeTreeNode parent;
    final List<Field> headers = new ArrayList<>();
    final List<MimeTreeNode> children = new ArrayList<>();

    byte[] preamble;
    byte[] epilogue;
    byte[] body;

    MimeTreeNode(String type, MimeTreeNode parent) {
        this.type = type;
        this.parent = parent;
    }

    public String getType() {
        return type;
    }

    public Stream<String> getHeaders(String name) {
        return headers.stream()
                .filter(header -> header.getName().equalsIgnoreCase(name))
                .map(Field::getBody);
    }

    public Optional<String> getHeader(String name) {
        return getHeaders(name)
                .reduce((a, b) -> { throw new IllegalStateException("Multiple occurrences of \"" + name + "\" header"); });
    }

    public String requireHeader(String name) {
        return getHeader(name)
                .orElseThrow(() -> new IllegalStateException("Missing \"" + name + "\" header"));
    }

    public int getChildCount() {
        return children.size();
    }

    public MimeTreeNode getChild(int index) {
        return children.get(index);
    }

    public byte[] getBody() {
        try {
            return MimeUtility.decode(
                    new ByteArrayInputStream(body),
                    getHeader("Content-Transfer-Encoding").orElse("7bit")
            ).readAllBytes();
        } catch (IOException | MessagingException e) {
            throw new RuntimeException(e);  // TODO
        }
    }

    public String decodeBody() {
        Matcher charsetMatcher = CHARSET_PATTERN.matcher(getHeader("Content-Type").orElse("text/plain"));
        Charset charset = charsetMatcher.find() ?
                Charset.forName(charsetMatcher.group(1)) :
                StandardCharsets.UTF_8;
        return new String(getBody(), charset);
    }

    public void walk(Function<MimeTreeNode, Boolean> visitor) {
        if (visitor.apply(this)) {
            for (MimeTreeNode child : children) {
                child.walk(visitor);
            }
        }
    }

    @Override
    public String toString() {
        return toString(0);
    }

    private String toString(int indentation) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" ".repeat(indentation));
        stringBuilder.append(type);
        stringBuilder.append(": {");
        stringBuilder.append(headers.stream().map(header -> header.getName() + ": " + header.getBody()).collect(Collectors.joining(", ")));
        stringBuilder.append("} ");

        if (body != null) {
            stringBuilder
                    .append('\n')
                    .append(" ".repeat(indentation + 2))
                    .append(new String(body).trim().replaceAll("\n", "\n" + " ".repeat(indentation + 2)))
                    .append('\n');
        }

        if (!children.isEmpty()) {
            stringBuilder.append('\n');
            for (MimeTreeNode child : children) {
                stringBuilder.append(child.toString(indentation + 2));
            }
        }

        return stringBuilder.toString();
    }
}
