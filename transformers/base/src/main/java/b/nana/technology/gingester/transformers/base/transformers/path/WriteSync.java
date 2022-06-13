package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class WriteSync implements Transformer<InputStream, Path> {

    private final Template pathTemplate;
    private final boolean mkdirs;
    private final StandardOpenOption[] openOptions;
    private final int bufferSize;

    public WriteSync(Parameters parameters) {
        pathTemplate = Context.newTemplate(parameters.path);
        mkdirs = parameters.mkdirs;
        openOptions = parameters.openOptions;
        bufferSize = parameters.bufferSize;
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<Path> out) throws Exception {

        Path path = Paths.get(pathTemplate.render(context));

        Path parent = path.getParent();
        if (mkdirs && parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        try (OutputStream output = Files.newOutputStream(path, openOptions)) {

            // TODO optionally wrap `output` in one or more DigestOutputStreams if instructed by Parameters

            write(in, output);
        }

        out.accept(context.stash(Map.of(
                "description", path,
                "path", Map.of(
                        "absolute", path.toAbsolutePath(),
                        "tail", path.getFileName()
                )
        )), path);
    }

    private void write(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
    }

    public static class Parameters {

        public TemplateParameters path;
        public boolean mkdirs = true;
        public StandardOpenOption[] openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE_NEW };
        public int bufferSize = 8192;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters path) {
            this.path = path;
        }
    }
}
