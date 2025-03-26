package b.nana.technology.gingester.transformers.claude;

import java.util.List;

public final class ErrorStringPruner {

    private ErrorStringPruner() {}

    public static String prune(String errorString) {
        StringBuilder pruned = new StringBuilder();
        List<String> lines = errorString.lines().toList();
        boolean ignoreAt = false;
        for (String line : lines) {
            if (line.isBlank()) continue;
            if (line.startsWith("\tat")) {
                if (!ignoreAt) ignoreAt = true;
                else continue;
            } else ignoreAt = false;
            pruned.append(line).append('\n');
        }
        return pruned.toString();
    }
}
