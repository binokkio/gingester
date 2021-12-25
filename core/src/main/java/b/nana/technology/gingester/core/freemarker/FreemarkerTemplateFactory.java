package b.nana.technology.gingester.core.freemarker;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.Version;

import java.io.IOException;
import java.util.function.Function;

public final class FreemarkerTemplateFactory {

    private static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_31;

    private FreemarkerTemplateFactory() {}

    public static FreemarkerTemplateWrapper createTemplate(String templateString, Function<Version, ObjectWrapper> objectWrapperFunction) {
        return createTemplate(templateString, objectWrapperFunction, Configuration.ANGLE_BRACKET_TAG_SYNTAX, Configuration.DOLLAR_INTERPOLATION_SYNTAX);
    }

    public static FreemarkerTemplateWrapper createAlternateSyntaxTemplate(String templateString, Function<Version, ObjectWrapper> objectWrapperFunction) {
        return createTemplate(templateString, objectWrapperFunction, Configuration.SQUARE_BRACKET_TAG_SYNTAX, Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
    }

    public static FreemarkerTemplateWrapper createTemplate(String templateString, Function<Version, ObjectWrapper> objectWrapperFunction, int tagSyntax, int interpolationSyntax) {
        Configuration configuration = new Configuration(FREEMARKER_VERSION);
        configuration.setTagSyntax(tagSyntax);
        configuration.setInterpolationSyntax(interpolationSyntax);
        configuration.setObjectWrapper(objectWrapperFunction.apply(FREEMARKER_VERSION));
        try {
            return new FreemarkerTemplateWrapper(new Template("", templateString, configuration));
        } catch (IOException e) {
            throw new RuntimeException(e);  // TODO
        }
    }
}
