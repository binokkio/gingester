package b.nana.technology.gingester.core.freemarker;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Version;

import java.io.IOException;
import java.util.function.Function;

public final class FreemarkerTemplateFactory {

    private static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_31;

    private FreemarkerTemplateFactory() {}

    public static FreemarkerTemplateWrapper createTemplate(String templateString, Function<Version, ObjectWrapper> objectWrapperFunction) {
        Configuration configuration = new Configuration(FREEMARKER_VERSION);
        configuration.setObjectWrapper(objectWrapperFunction.apply(FREEMARKER_VERSION));
        try {
            return new FreemarkerTemplateWrapper(new freemarker.template.Template("", templateString, configuration));
        } catch (IOException e) {
            throw new RuntimeException(e);  // TODO
        }
    }
}
