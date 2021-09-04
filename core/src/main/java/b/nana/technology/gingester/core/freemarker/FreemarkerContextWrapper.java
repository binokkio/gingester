package b.nana.technology.gingester.core.freemarker;

import b.nana.technology.gingester.core.context.Context;
import freemarker.template.*;

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
                return handleUnknownType(context.fetch(name)
                        .findFirst()
                        .orElseGet(() -> context.rewind(name)));
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
    }
}
