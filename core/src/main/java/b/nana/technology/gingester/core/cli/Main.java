package b.nana.technology.gingester.core.cli;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.configuration.GingesterConfiguration;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gingester.class);

    private Main() {}

    public static void main(String[] args) {
        if (args.length > 0 && Arrays.stream(args).noneMatch(s -> s.equals("-h") || s.equals("--help"))) {
            boolean useShutdownHook = Arrays.asList(args).contains("-gs") || Arrays.asList(args).contains("--graceful-shutdown");
            if (useShutdownHook) args = Arrays.stream(args).filter(s -> !s.equals("-gs") && !s.equals("--graceful-shutdown")).toArray(String[]::new);
            GingesterConfiguration configuration = CliParser.parse(args);
            if (configuration.report == null) configuration.report = 2;
            Gingester gingester = new Gingester(configuration);
            AtomicBoolean stopping = new AtomicBoolean();
            Thread stop = new Thread(() -> {
                LOGGER.warn("Received shutdown signal, starting graceful shutdown");
                try {
                    stopping.set(true);
                    gingester.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();  // TODO
                }
            });
            if (useShutdownHook) Runtime.getRuntime().addShutdownHook(stop);
            gingester.run();
            if (useShutdownHook && !stopping.get()) Runtime.getRuntime().removeShutdownHook(stop);
        } else {
            printHelp();
        }
    }

    private static void printHelp() {

        try {
            System.out.println(new String(requireNonNull(
                    Main.class.getResourceAsStream("/gingester/core/help.txt"),
                    "/gingester/core/help.txt resource missing"
            ).readAllBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        System.out.println("\nAvailable transformers:\n");
        TransformerFactory.getTransformerHelps().forEach(help ->
                System.out.println("    " + help));
    }
}
