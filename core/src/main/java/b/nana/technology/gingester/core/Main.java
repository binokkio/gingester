package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.transformer.TransformerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

import static java.util.Objects.requireNonNull;

public final class Main {

    private Main() {}

    public static void main(String[] arrgs) {

        List<String> args = new LinkedList<>(Arrays.asList(arrgs));

        boolean help = filter(args, Set.of("-h", "--help"));

        if (help) {
            printHelp();
        } else {

            boolean see = filter(args, Set.of("--see"));

            FlowRunner flowRunner = new FlowBuilder()
                    .setReportIntervalSeconds(2)
                    .cli(args.toArray(new String[0]))
                    .build();

            if (see) {
                System.out.println(flowRunner);
            } else {
                flowRunner.run();
            }
        }
    }

    private static boolean filter(List<String> list, Set<String> filters) {
        Iterator<String> iterator = list.iterator();
        boolean filtered = false;
        while (iterator.hasNext()) {
            if (filters.contains(iterator.next())) {
                iterator.remove();
                filtered = true;
            }
        }
        return filtered;
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
