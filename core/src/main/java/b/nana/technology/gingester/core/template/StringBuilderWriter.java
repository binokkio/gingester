package b.nana.technology.gingester.core.template;

import java.io.Writer;

public class StringBuilderWriter extends Writer {

    private final StringBuilder builder = new StringBuilder();

    @Override
    public Writer append(char c) {
        builder.append(c);
        return this;
    }

    @Override
    public Writer append(CharSequence csq) {
        builder.append(csq);
        return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) {
        builder.append(csq, start, end);
        return this;
    }

    @Override
    public void write(char[] cbuf) {
        builder.append(cbuf);
    }

    @Override
    public void write(char[] chars, int off, int len) {
        builder.append(chars, off, len);
    }

    @Override
    public void write(int c) {
        builder.append(c);
    }

    @Override
    public void write(String str) {
        builder.append(str);
    }

    @Override
    public void write(String str, int off, int len) {
        builder.append(str, off, off + len);
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    @Override
    public String toString() {
        return builder.toString();
    }
}