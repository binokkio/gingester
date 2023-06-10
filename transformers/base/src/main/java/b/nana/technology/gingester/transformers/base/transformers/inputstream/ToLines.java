package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.string.CharsetTransformer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@Names(1)
public final class ToLines extends CharsetTransformer<InputStream, String> {

    public ToLines(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<String> out) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, getCharset()));
        String line;
        for (long i = 0; (line = bufferedReader.readLine()) != null; i++) {
            out.accept(context.stash("description", i), line);
        }
    }
}
