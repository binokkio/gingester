package b.nana.technology.gingester.core.cli;

import java.util.ArrayList;
import java.util.List;

public final class CliSplitter {

    private CliSplitter() {}

    public static String[] split(String cli) {

        List<String> args = new ArrayList<>();

        StringBuilder arg = new StringBuilder();
        char quote = 0;
        boolean escape = false;

        for (int i = 0; i < cli.length(); i++) {

            char c = cli.charAt(i);

            if (c == '\\' && !escape) {
                escape = true;
                continue;
            }

            if (arg.length() == 0 && quote == 0) {
                if ((c == '"' || c == '\'') && !escape) {
                    quote = c;
                } else if (!Character.isWhitespace(c)) {
                    arg.append(c);
                }
            } else {
                if (quote == 0 && Character.isWhitespace(c) && !escape) {
                    args.add(arg.toString());
                    arg = new StringBuilder();
                } else if (c == quote && !escape) {
                    quote = 0;
                    args.add(arg.toString());
                    arg = new StringBuilder();
                } else {
                    arg.append(c);
                }
            }

            escape = false;
        }

        if (arg.length() > 0) args.add(arg.toString());

        return args.toArray(new String[0]);
    }
}
