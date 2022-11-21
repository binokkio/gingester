package b.nana.technology.gingester.core.cli;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BlockCommentRemover {

    private static final Pattern BLOCK_COMMENT = Pattern.compile("(?:^|\\s)(\\+{2,})(?:\\s|$)");
    private static final Pattern BLOCK_COMMENT_ARG = Pattern.compile("\\+{2,}");

    static String removeBlockComments(String source) {
        int level = 0;
        int begin = -1;
        int end = -1;
        Matcher matcher = BLOCK_COMMENT.matcher(source);
        while (matcher.find()) {
            int foundLevel = matcher.group(1).length();
            if (foundLevel > level) {
                level = foundLevel;
                begin = matcher.start(1);
                end = -1;
            } else if (foundLevel == level && end == -1) {
                end = matcher.start(1) + foundLevel;
            }
        }
        if (level > 0) {
            String result = source.substring(0, begin);
            if (end != -1) result += source.substring(end);
            return removeBlockComments(result);
        } else {
            return source;
        }
    }

    static String[] removeBlockComments(String[] args) {
        int level = 0;
        int begin = -1;
        int end = -1;
        for (int i = 0; i < args.length; i++) {
            Matcher matcher = BLOCK_COMMENT_ARG.matcher(args[i]);
            if (matcher.matches()) {
                int foundLevel = matcher.group().length();
                if (foundLevel > level) {
                    level = foundLevel;
                    begin = i;
                    end = -1;
                } else if (foundLevel == level && end == -1) {
                    end = i;
                }
            }
        }
        if (level > 0) {
            String[] result;
            if (end == -1) {
                result = new String[begin];
                System.arraycopy(args, 0, result, 0, begin);
            } else {
                result = new String[begin + (args.length - end - 1)];
                System.arraycopy(args, 0, result, 0, begin);
                System.arraycopy(args, end + 1, result, begin, args.length - end - 1);
            }
            return removeBlockComments(result);
        } else {
            return args;
        }
    }
}
