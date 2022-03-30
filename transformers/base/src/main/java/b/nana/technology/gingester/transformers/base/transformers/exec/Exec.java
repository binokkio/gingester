package b.nana.technology.gingester.transformers.base.transformers.exec;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Names(1)
public final class Exec implements Transformer<InputStream, InputStream> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Exec.class);

    private final ExecutorService errDrainer = Executors.newCachedThreadPool();

    private final Template commandTemplate;
    private final Template workDirTemplate;

    public Exec(Parameters parameters) {
        commandTemplate = Context.newTemplate(parameters.command);
        workDirTemplate = Context.newTemplate(parameters.workDir);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingAsync();
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {

        String command = commandTemplate.render(context);

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command, null, new File(workDirTemplate.render(context)).getAbsoluteFile());

        out.accept(context.stash("description", command), process.getInputStream());

        // TODO emit stderr instead, to a transformer specified by a parameter
        BufferedReader processStdErr = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
        errDrainer.submit(() -> {
            while (true) {
                try {
                    String line = processStdErr.readLine();
                    if (line == null) break;
                    LOGGER.info("{}: {}", command, line);
                } catch (IOException e) {
                    throw new RuntimeException("Reading " + command + " stderr threw", e);
                }
            }
        });

        OutputStream processStdIn = process.getOutputStream();
        in.transferTo(processStdIn);
        processStdIn.close();

        int resultCode = process.waitFor();
        // TODO throw an exception on non-zero resultCode?
    }

    @Override
    public void close() {
        errDrainer.shutdown();
    }

    public static class Parameters {

        public TemplateParameters command;
        public TemplateParameters workDir = new TemplateParameters("");

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters command) {
            this.command = command;
        }
    }
}
