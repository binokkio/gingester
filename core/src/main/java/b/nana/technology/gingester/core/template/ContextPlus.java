package b.nana.technology.gingester.core.template;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

import java.util.Collections;
import java.util.Map;

import static b.nana.technology.gingester.core.template.FreemarkerTemplateFactory.JACKSON_WRAPPER;

final class ContextPlus implements TemplateHashModel {

    private final Context context;
    private final Object in;
    private final Map<String, Object> extras;

    ContextPlus(Context context) {
        this(context, null, Collections.emptyMap());
    }

    ContextPlus(Context context, Object in) {
        this(context, in, Collections.emptyMap());
    }

    ContextPlus(Context context, Object in, Map<String, Object> extras) {
        this.context = context;
        this.in = in;
        this.extras = extras;
    }

    @Override
    public TemplateModel get(String key) {
        if (key.equals("__in__")) {
            return JACKSON_WRAPPER.wrap(in);
        } else {
            Object extra = extras.get(key);
            if (extra != null) {
                return JACKSON_WRAPPER.wrap(extra);
            } else {
                return JACKSON_WRAPPER.wrap(context.fetch(new FetchKey(key, true)).orElse(null));
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
