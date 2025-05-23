package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.nio.file.Files;
import java.nio.file.Path;

public final class MakeDir implements Transformer<Path, Path> {

    @Override
    public void transform(Context context, Path in, Receiver<Path> out) throws Exception {
        Files.createDirectories(in);
        out.accept(context, in);
    }
}
