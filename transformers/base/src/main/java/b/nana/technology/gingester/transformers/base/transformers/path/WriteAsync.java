package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class WriteAsync implements Transformer<OutputStreamWrapper, Path> {

    private final Template pathTemplate;
    private final boolean mkdirs;
    private final StandardOpenOption[] openOptions;
    private final int bufferSize;

    public WriteAsync(Parameters parameters) {
        pathTemplate = Context.newTemplate(parameters.path);
        mkdirs = parameters.mkdirs;
        openOptions = parameters.openOptions;
        bufferSize = parameters.bufferSize;
    }

    @Override
    public void transform(Context context, OutputStreamWrapper in, Receiver<Path> out) throws Exception {

        String pathString = pathTemplate.render(context);
        Path path = Paths.get(pathString);

        Path parent = path.getParent();
        if (mkdirs && parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        OutputStream outputStream = Files.newOutputStream(path, openOptions);
        if (bufferSize != 0) outputStream = new BufferedOutputStream(outputStream, bufferSize);
        in.wrap(outputStream);

        out.accept(context.stash(Map.of(
                "monitor", in,
                "description", pathString,
                "path", Map.of(
                        "absolute", path.toAbsolutePath(),
                        "tail", path.getFileName()
                )
        )), path);
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
