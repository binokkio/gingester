package b.nana.technology.gingester.transformers.base.common.json;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PathToPointer {

    private static final Pattern PART_PATTERN = Pattern.compile("\\[(.*?)]");

    private PathToPointer() {

    }

    /**
     * Turns json paths from com.jayway.jsonpath when configured with Option.AS_PATH_LIST
     * into JSON pointers.
     */
    public static String jsonPathToPointer(String jsonPath) {

        StringBuilder result = new StringBuilder();
        Matcher matcher = PART_PATTERN.matcher(jsonPath);

        int offset = 0;
        while (matcher.find(offset)) {
            offset = matcher.end();

            result.append('/');

            String part = matcher.group(1);
            if (part.startsWith("'")) {
                result.append(part, 1, part.length() - 1);
            } else {
                result.append(part);
            }
        }
        return result.toString();
    }
}
