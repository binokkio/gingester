package b.nana.technology.gingester.core.template;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

public class FreemarkerContextWrapper extends FreemarkerJacksonWrapper {

    public FreemarkerContextWrapper(Version freemarkerVersion) {
        super(freemarkerVersion);
    }

    @Override
    public TemplateModel wrap(Object object) throws TemplateModelException {
        if (object instanceof Context) {
            return handleContext((Context) object);
        } else {
            return super.wrap(object);
        }
    }

    private TemplateModel handleContext(Context context) {

        return new TemplateHashModel() {

            public TemplateModel get(String name) throws TemplateModelException {
                return wrap(context.fetch(new FetchKey(name, true)).orElse(null));
            }

            public boolean isEmpty() {
                return false;
            }
        };
    }
}
