package b.nana.technology.gingester.transformers.smtp.mimetree;

import org.apache.james.mime4j.stream.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MimeTreeNode {

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

    public Stream<String> getHeaderValues(String name) {
        return headers.stream()
                .filter(header -> header.getName().equalsIgnoreCase(name))
                .map(Field::getBody);
    }

    public byte[] getBody() {
        return body;
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
