package b.nana.technology.gingester.transformers.smtp.mimetree;

import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;

import java.io.IOException;
import java.io.InputStream;

public class MimeTreeBuilder implements ContentHandler {

    private MimeTreeNode root;
    private MimeTreeNode pointer;

    private void startX(String type) {
        MimeTreeNode child = new MimeTreeNode(type, pointer);
        if (root == null) root = child;
        else if (pointer == null) throw new IllegalStateException("Multiple root nodes");
        else pointer.children.add(child);
        pointer = child;
    }

    @Override
    public void startMessage() {
        startX("message");
    }

    @Override
    public void endMessage() {
        pointer = pointer.parent;
    }

    @Override
    public void startBodyPart() {
        startX("bodyPart");
    }

    @Override
    public void endBodyPart() {
        pointer = pointer.parent;
    }

    @Override
    public void startMultipart(BodyDescriptor bodyDescriptor) {
        startX("multiPart");
    }

    @Override
    public void endMultipart() {
        pointer = pointer.parent;
    }

    @Override
    public void startHeader() {
        // ignore
    }

    @Override
    public void endHeader() {
        // ignore
    }

    @Override
    public void field(Field field) {
        pointer.headers.add(field);
    }

    @Override
    public void preamble(InputStream inputStream) throws IOException {
        pointer.preamble = inputStream.readAllBytes();
    }

    @Override
    public void epilogue(InputStream inputStream) throws IOException {
        pointer.epilogue = inputStream.readAllBytes();
    }

    @Override
    public void body(BodyDescriptor bodyDescriptor, InputStream inputStream) throws IOException {
        pointer.body = inputStream.readAllBytes();
    }

    @Override
    public void raw(InputStream inputStream) {
        throw new UnsupportedOperationException("Unexpected call to `raw`");
    }

    public MimeTreeNode getRoot() {
        return root;
    }
}
