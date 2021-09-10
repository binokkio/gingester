package b.nana.technology.gingester.transformers.base.transformers.exec;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.SetupControls;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.File;
import java.io.InputStream;

public class Exec implements Transformer<InputStream, InputStream> {

    private final Context.Template commandTemplate;
    private final Context.Template workDirTemplate;

    public Exec(Parameters parameters) {
        commandTemplate = Context.newTemplate(parameters.command);
        workDirTemplate = Context.newTemplate(parameters.workDir);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireDownstreamAsync = true;
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {

        String command = commandTemplate.render(context);

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);

        out.accept(context.stash("description", command), process.getInputStream());
        // TODO handle process.getErrorStream(), maybe emit both to different links?

        in.transferTo(process.getOutputStream());

        int resultCode = process.waitFor();
        // TODO throw an exception on non-zero resultCode?
    }

    public static class Parameters {

        public String command;
        public String workDir = "";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String command) {
            this.command = command;
        }
    }
}
