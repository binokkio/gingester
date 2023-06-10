package b.nana.technology.gingester.transformers.poi;

import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.BiConsumer;

public class SheetSplittingOutputStream extends OutputStream {

    private static final byte[] ESCAPED_QUOTE = new byte[] { '"', '"'};

    private final boolean escapeQuotes;
    private final BiConsumer<String, OutputStreamWrapper> sheetConsumer;
    private OutputStream current;
    private boolean skipNewline = true;

    public SheetSplittingOutputStream(boolean escapeQuotes, BiConsumer<String, OutputStreamWrapper> sheetConsumer) {
        this.escapeQuotes = escapeQuotes;
        this.sheetConsumer = sheetConsumer;
        this.current = nullOutputStream();
    }

    @Override
    public void write(int i) throws IOException {
        if (i != '\n') {
            current.write(i);
            skipNewline = false;
        } else if (!skipNewline) {
            current.write(i);
            skipNewline = true;
        }
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {

        if (len == 1) {
            write(bytes[off]);
            return;
        } else if (len > 1) {

            // if this is a new sheet
            boolean isSheetName1 = bytes[off + len - 2] == ':' && bytes[off + len - 1] == '\n';
            boolean isSheetName2 = bytes[off + len - 2] == ']' && bytes[off + len - 1] == ':';
            if (isSheetName1 || isSheetName2) {

                // extract sheet name
                int lastIndexOf = len - 1;
                for (; lastIndexOf >= 0; lastIndexOf--)
                    if (bytes[off + lastIndexOf] == '[')
                        break;
                String sheetName = new String(bytes, off, lastIndexOf - 1);

                // close current output, start a new one
                current.close();
                OutputStreamWrapper outputStreamWrapper = new OutputStreamWrapper();
                current = outputStreamWrapper;
                sheetConsumer.accept(sheetName, outputStreamWrapper);
                return;
            }

            // if this is a quoted string, write it out with nested double quotes escaped
            if (escapeQuotes && bytes[off] == '"' && bytes[off + len - 1] == '"') {
                current.write('"');
                for (int i = 1; i < len - 1; i++) {
                    byte b = bytes[off + i];
                    if (b == '"') current.write(ESCAPED_QUOTE);
                    else current.write(b);
                }
                current.write('"');
                return;
            }
        }

        current.write(bytes, off, len);
    }

    @Override
    public void flush() throws IOException {
        current.flush();
    }

    @Override
    public void close() throws IOException {
        current.close();
    }
}
