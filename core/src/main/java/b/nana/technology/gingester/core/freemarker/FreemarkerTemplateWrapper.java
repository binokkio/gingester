package b.nana.technology.gingester.core.freemarker;

import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;

public final class FreemarkerTemplateWrapper {

    private final freemarker.template.Template template;

    public FreemarkerTemplateWrapper(freemarker.template.Template template) {
        this.template = template;
    }

    public String render(Object object) {
        StringWriter stringWriter = new StringWriter();
        try {
            template.process(object, stringWriter);
        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e);  // TODO
        }
        return stringWriter.toString();
    }
}
