package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.nio.file.Files;
import java.nio.file.Path;

public class Size implements Transformer<Path, Long> {

    @Override
    public void transform(Context context, Path in, Receiver<Long> out) throws Exception {
        out.accept(context, Files.size(in));
    }
}
