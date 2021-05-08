package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ToString extends Transformer<InputStream, String> {

    private static final Charset CHARSET = StandardCharsets.UTF_8;  // TODO add to Parameters

    private final String delimiter;

    public ToString() {
        delimiter = null;
    }

    public ToString(Parameters parameters) {
        super(parameters);
        delimiter = parameters.delimiter;
    }

    @Override
    protected void transform(Context context, InputStream input) throws IOException {
        if (delimiter == null) {
            emit(context, new String(input.readAllBytes(), CHARSET));
        } else {
            Scanner scanner = new Scanner(new InputStreamReader(input, CHARSET)).useDelimiter(delimiter);
            long counter = 0;
            while (scanner.hasNext()) {
                emit(
                        context.extend(this).description(++counter),
                        scanner.next()
                );
            }
        }
    }

    public static class Parameters {
        public String delimiter;
    }
}
