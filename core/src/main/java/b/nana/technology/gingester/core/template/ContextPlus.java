package b.nana.technology.gingester.core.template;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.util.Collections;
import java.util.Map;

import static b.nana.technology.gingester.core.template.FreemarkerTemplateFactory.OBJECT_WRAPPER;

public final class ContextPlus implements TemplateHashModel {

    private final Context context;
    private final Object in;

    public ContextPlus(Context context) {
        this(context, null);
    }

    public ContextPlus(Context context, Object in) {
        this.context = context;
        this.in = in;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("__in__")) {
            return OBJECT_WRAPPER.wrap(in);
        } else {
            return OBJECT_WRAPPER.wrap(context.fetch(new FetchKey(key, true)).orElse(null));
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
