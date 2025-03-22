package b.nana.technology.gingester.core.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import freemarker.template.*;
import freemarker.template.utility.DeepUnwrap;

import java.util.Map;
import java.util.Set;

public final class FreemarkerTemplateFactory {

    private static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_31;
    private static final String DEFAULT_DIRECTIVE = "default";
    private static final String KWARGS_DIRECTIVE = "kwargs";
    private static final Set<String> NOT_KWARGS = Set.of(DEFAULT_DIRECTIVE, KWARGS_DIRECTIVE, "capture_output", "compress", "html_escape", "normalize_newlines", "xml_escape");

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static final ObjectWrapper OBJECT_WRAPPER = new ObjectWrapper(FREEMARKER_VERSION);

    private static final Configuration CONFIGURATION;
    private static final Configuration CLI_CONFIGURATION;
    static {
        CONFIGURATION = new Configuration(FREEMARKER_VERSION);
        CONFIGURATION.setLogTemplateExceptions(false);
        CONFIGURATION.setTagSyntax(Configuration.ANGLE_BRACKET_TAG_SYNTAX);
        CONFIGURATION.setInterpolationSyntax(Configuration.DOLLAR_INTERPOLATION_SYNTAX);
        CONFIGURATION.setOutputEncoding("UTF-8");
        CONFIGURATION.setNumberFormat("computer");
        CONFIGURATION.setBooleanFormat("c");
        CONFIGURATION.setObjectWrapper(OBJECT_WRAPPER);
        CONFIGURATION.setAPIBuiltinEnabled(true);

        CLI_CONFIGURATION = new Configuration(FREEMARKER_VERSION);
        CLI_CONFIGURATION.setLogTemplateExceptions(false);
        CLI_CONFIGURATION.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        CLI_CONFIGURATION.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        CLI_CONFIGURATION.setOutputEncoding("UTF-8");
        CLI_CONFIGURATION.setNumberFormat("computer");
        CLI_CONFIGURATION.setBooleanFormat("c");
        CLI_CONFIGURATION.setObjectWrapper(OBJECT_WRAPPER);
        CLI_CONFIGURATION.setAPIBuiltinEnabled(true);
        CLI_CONFIGURATION.setSharedVariable(DEFAULT_DIRECTIVE, (TemplateDirectiveModel) (env, params, loopVars, body) -> {
            // noinspection unchecked
            ((Map<String, TemplateModel>) params).forEach((variableName, defaultValue) -> {
                try {
                    if (env.getVariable(variableName) == null) {
                        env.setVariable(variableName, defaultValue);
                    }
                } catch (TemplateModelException e) {
                    throw new RuntimeException(e);
                }
            });
        });
        CLI_CONFIGURATION.setSharedVariable(KWARGS_DIRECTIVE, (TemplateDirectiveModel) (env, params, loopVars, body) -> {

            ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

            //noinspection unchecked
            for (String variableName : ((Set<String>) env.getKnownVariableNames())) {
                if (!NOT_KWARGS.contains(variableName)) {
                    objectNode.set(variableName, OBJECT_MAPPER.valueToTree(DeepUnwrap.unwrap(env.getVariable(variableName))));
                }
            }

            env.getOut().write(objectNode.toString().replaceAll("\\\\", "\\\\\\\\"));
        });
    }

    private FreemarkerTemplateFactory() {}

    public static FreemarkerTemplateWrapper createTemplate(String name, String templateString) {
        return createTemplate(name, templateString, CONFIGURATION);
    }

    public static FreemarkerTemplateWrapper createCliTemplate(String name, String templateString) {
        return createTemplate(name, templateString, CLI_CONFIGURATION);
    }

    private static FreemarkerTemplateWrapper createTemplate(String name, String templateString, Configuration configuration) {
        return new FreemarkerTemplateWrapper(name, templateString, configuration);
    }
}
