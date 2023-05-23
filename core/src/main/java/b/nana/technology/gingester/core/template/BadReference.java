package b.nana.technology.gingester.core.template;

import com.fasterxml.jackson.databind.JsonNode;
import freemarker.core.InvalidReferenceException;

public class BadReference extends RuntimeException {

    public BadReference(InvalidReferenceException cause, Object dataModel) {
        super(getMessage(cause, dataModel));
        setStackTrace(new StackTraceElement[0]);
    }

    private static String getMessage(InvalidReferenceException cause, Object dataModel) {

        String blamedExpressionString = cause.getBlamedExpressionString();
        StringBuilder message = new StringBuilder()
                .append('`')
                .append(blamedExpressionString)
                .append("` evaluated to null or missing");

        String templateSourceName = cause.getTemplateSourceName();
        if (!"STRING".equals(templateSourceName)) {
            message
                    .append(" in template ")
                    .append(templateSourceName);
        }

        message
                .append(" at line ")
                .append(cause.getLineNumber())
                .append(", column ")
                .append(cause.getColumnNumber())
                .append(".\n");

        if (dataModel instanceof ContextPlus) {
            if (blamedExpressionString.startsWith("__in__")) {
                message.append("Input: ");
                Object in = ((ContextPlus) dataModel).in;
                if (in instanceof JsonNode) {
                    message.append(((JsonNode) in).toPrettyString());
                } else {
                    message.append(in);
                }
            } else {
                message.append("Context: ");
                message.append(((ContextPlus) dataModel).context.prettyStash(10));
            }
        } else {
            message.append("Data: ");
            if (dataModel instanceof JsonNode) {
                message.append(((JsonNode) dataModel).toPrettyString());
            } else {
                message.append(dataModel);
            }
        }

        return message.toString();
    }
}
