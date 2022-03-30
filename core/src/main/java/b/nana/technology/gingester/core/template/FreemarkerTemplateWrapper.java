package b.nana.technology.gingester.core.template;

import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;

public final class FreemarkerTemplateWrapper {

    private final Template template;

    public FreemarkerTemplateWrapper(Template template) {
        this.template = template;
    }

    public String render() {
        return render(null);
    }

    public String render(Object object) {
        StringWriter stringWriter = new StringWriter();
        try {
            template.process(object, stringWriter);
        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e.getMessage());  // TODO
        }
        return stringWriter.toString();
    }
}
