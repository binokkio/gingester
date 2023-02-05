package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Pure
public final class ToOutputStream implements Transformer<Path, OutputStream> {

    @Override
    public void transform(Context context, Path in, Receiver<OutputStream> out) throws Exception {
        out.accept(context, Files.newOutputStream(in));
    }
}
