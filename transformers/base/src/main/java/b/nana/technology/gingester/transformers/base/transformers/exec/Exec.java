package b.nana.technology.gingester.transformers.base.transformers.exec;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.cli.CliSplitter;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Names(1)
public final class Exec implements Transformer<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Exec.class);

    private final ExecutorService drainers = Executors.newCachedThreadPool();

    private final Template commandTemplate;
    private final TemplateMapper<File> workDirTemplate;
    private final Parameters.StreamHandling stdout;
    private final Parameters.StreamHandling stderr;
    private final boolean stdin;
    private final boolean logPid;

    public Exec(Parameters parameters) {
        commandTemplate = Context.newTemplate(parameters.command);
        workDirTemplate = Context.newTemplateMapper(parameters.workDir, s -> new File(s).getAbsoluteFile());
        stdout = parameters.stdout;
        stderr = parameters.stderr;
        stdin = parameters.stdin;
        logPid = parameters.logPid;
    }

    private boolean yieldsStream() {
        return stdout == Parameters.StreamHandling.YIELD || stderr == Parameters.StreamHandling.YIELD;
    }

    @Override
    public void setup(SetupControls controls) {
        if (yieldsStream())
            controls.requireOutgoingAsync();
    }

    @Override
    public Class<?> getInputType() {
        return stdin ? InputStream.class : Object.class;
    }

    @Override
    public Object getOutputType() {
        return yieldsStream() ?
                InputStream.class :
                Object.class;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        String command = commandTemplate.render(context, in);
        String[] args = CliSplitter.split(command);
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(args, null, workDirTemplate.render(context, in));
        long pid = process.pid();

        if (logPid)
            LOGGER.info("Process {} started: {}", pid, command);

        handleStream(pid, command, "stdout", stdout, process.getInputStream(), context, out);
        handleStream(pid, command, "stderr", stderr, process.getErrorStream(), context, out);

        OutputStream processStdIn = process.getOutputStream();
        if (stdin) ((InputStream) in).transferTo(processStdIn);
        processStdIn.close();

        int resultCode = process.waitFor();
        // TODO throw an exception on non-zero resultCode?

        if (!yieldsStream())
            out.accept(context, "exec finish signal");
    }

    private void handleStream(long pid, String command, String streamName, Parameters.StreamHandling handling, InputStream stream, Context context, Receiver<Object> out) {
        switch (handling) {

            case YIELD:
                out.accept(context.stash(Map.of(
                        "description", command + " (" + streamName + ")",
                        "command", command,
                        "stream", streamName
                )), stream);
                break;

            case LOG:
                drainers.submit(() -> {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                        String prefix = pid + " " + streamName + ": ";
                        String line;
                        while ((line = reader.readLine()) != null) {
                            LOGGER.info("{}: {}", prefix, line);
                        }
                    } catch (IOException e) {
                        // TODO
                        throw new RuntimeException("Reading " + command + " stderr threw", e);
                    }
                });
                break;

            case IGNORE:
                drainers.submit(() -> {
                    try {
                        while (stream.skip(Long.MAX_VALUE) > 0) ;
                    } catch (IOException e) {
                        // TODO
                    }
                });
                break;
        }
    }

    @Override
    public void close() {
        drainers.shutdown();
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"command", "workDir", "stdout", "stderr"})
    public static class Parameters {

        public TemplateParameters command;
        public TemplateParameters workDir = new TemplateParameters("", true);
        public StreamHandling stdout = StreamHandling.YIELD;
        public StreamHandling stderr = StreamHandling.LOG;
        public boolean stdin = true;
        public boolean logPid = false;

        public enum StreamHandling {
            YIELD,
            LOG,
            IGNORE
        }
    }
}
