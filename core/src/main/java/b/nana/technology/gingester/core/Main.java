package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.transformer.TransformerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        if (args.length == 0 || Arrays.stream(args).anyMatch(s -> s.equals("-h") || s.equals("--help"))) {
            printHelp();
        } else {
            new FlowBuilder()
                    .setReportIntervalSeconds(2)
                    .cli(args)
                    .run();
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
