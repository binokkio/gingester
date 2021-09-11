package b.nana.technology.gingester.core.freemarker;

import b.nana.technology.gingester.core.controller.Context;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

public class FreemarkerContextWrapper extends FreemarkerJacksonWrapper {

    public FreemarkerContextWrapper(Version freemarkerVersion) {
        super(freemarkerVersion);
    }

    @Override
    protected TemplateModel handleUnknownType(Object object) throws TemplateModelException {
        if (object instanceof Context) {
            return handleContext((Context) object);
        } else {
            return super.handleUnknownType(object);
        }
    }

    private TemplateModel handleContext(Context context) {

        return new TemplateHashModel() {

            @Override
            public TemplateModel get(String name) throws TemplateModelException {
                return handleUnknownType(context.fetch(name).findFirst().orElse(TemplateModel.NOTHING));
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
    }
}
