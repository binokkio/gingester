package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class WriteAsync implements Transformer<OutputStreamWrapper, Path> {

    private final Context.Template pathTemplate;
    private final boolean mkdirs;
    private final StandardOpenOption[] openOptions;

    public WriteAsync(Parameters parameters) {
        pathTemplate = Context.newTemplate(parameters.path);
        mkdirs = parameters.mkdirs;
        openOptions = parameters.openOptions;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingAsync();
    }

    @Override
    public void transform(Context context, OutputStreamWrapper in, Receiver<Path> out) throws Exception {

        String pathString = pathTemplate.render(context);
        Path path = Paths.get(pathString);

        Path parent = path.getParent();
        if (mkdirs && parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        in.wrap(Files.newOutputStream(path, openOptions));

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

        public String path;
        public boolean mkdirs = true;
        public StandardOpenOption[] openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE_NEW };

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String path) {
            this.path = path;
        }
    }
}
