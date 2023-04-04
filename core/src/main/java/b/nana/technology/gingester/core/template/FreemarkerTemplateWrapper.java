package b.nana.technology.gingester.core.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public final class FreemarkerTemplateWrapper {

    private final String raw;
    private final Template template;

    public FreemarkerTemplateWrapper(String name, String raw, Configuration configuration) {
        try {
            this.raw = raw;
            this.template = new Template(name, raw, configuration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getRaw() {
        return raw;
    }

    public String render() {
        return render(null);
    }

    public String render(Object object) {
        Writer stringWriter = new StringWriter();  // TODO replace with custom Writer backed by StringBuilder?
        try {
            template.process(object, stringWriter);
        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e.getMessage());  // TODO
        }
        return stringWriter.toString();
    }
}
