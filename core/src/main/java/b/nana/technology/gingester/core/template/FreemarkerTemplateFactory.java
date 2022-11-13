package b.nana.technology.gingester.core.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;

import java.io.IOException;

public final class FreemarkerTemplateFactory {

    private static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_31;
    public static final FreemarkerJacksonWrapper JACKSON_WRAPPER = new FreemarkerJacksonWrapper(FREEMARKER_VERSION);

    private FreemarkerTemplateFactory() {}

    public static FreemarkerTemplateWrapper createTemplate(String templateString) {
        return createTemplate("", templateString, Configuration.ANGLE_BRACKET_TAG_SYNTAX, Configuration.DOLLAR_INTERPOLATION_SYNTAX);
    }

    public static FreemarkerTemplateWrapper createCliTemplate(String name, String templateString) {
        return createTemplate(name, templateString, Configuration.SQUARE_BRACKET_TAG_SYNTAX, Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
    }

    private static FreemarkerTemplateWrapper createTemplate(String name, String templateString, int tagSyntax, int interpolationSyntax) {
        Configuration configuration = new Configuration(FREEMARKER_VERSION);
        configuration.setLogTemplateExceptions(false);
        configuration.setTagSyntax(tagSyntax);
        configuration.setInterpolationSyntax(interpolationSyntax);
        configuration.setNumberFormat("computer");
        configuration.setBooleanFormat("c");
        configuration.setObjectWrapper(JACKSON_WRAPPER);
        try {
            return new FreemarkerTemplateWrapper(new Template(name, templateString, configuration));
        } catch (IOException e) {
            throw new RuntimeException(e);  // TODO
        }
    }
}
