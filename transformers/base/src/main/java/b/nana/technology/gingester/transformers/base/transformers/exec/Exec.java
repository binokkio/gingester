package b.nana.technology.gingester.transformers.base.transformers.exec;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.cli.CliSplitter;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
    private final TemplateMapper<File> workDirTemplate;

    public Exec(Parameters parameters) {
        commandTemplate = Context.newTemplate(parameters.command);
        workDirTemplate = Context.newTemplateMapper(parameters.workDir, s -> new File(s).getAbsoluteFile());
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingAsync();
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {

        String command = commandTemplate.render(context, in);
        String[] args = CliSplitter.split(command);

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(args, null, workDirTemplate.render(context, in));

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

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, command -> o("command", command));
                rule(JsonNode::isObject, o -> o.has("template") ? o("command", o) : o);
            }
        }

        public TemplateParameters command;
        public TemplateParameters workDir = new TemplateParameters("", true);
    }
}
