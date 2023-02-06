package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Pure
public final class ToOutputStream implements Transformer<Path, OutputStream> {

    private final boolean mkdirs;
    private final StandardOpenOption[] openOptions;
    private final int bufferSize;

    public ToOutputStream(Parameters parameters) {
        mkdirs = parameters.mkdirs;
        openOptions = parameters.openOptions;
        bufferSize = parameters.bufferSize;
    }

    @Override
    public void transform(Context context, Path in, Receiver<OutputStream> out) throws Exception {

        Path parent = in.getParent();
        if (mkdirs && parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        OutputStream outputStream = Files.newOutputStream(in, openOptions);
        if (bufferSize != 0) outputStream = new BufferedOutputStream(outputStream, bufferSize);

        out.accept(context, outputStream);
    }

    public static class Parameters {
        public boolean mkdirs = true;
        public StandardOpenOption[] openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE_NEW };
        public int bufferSize = 8192;
    }
}
