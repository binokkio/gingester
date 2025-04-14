package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.annotations.Experimental;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.annotations.Pure;

import java.util.ArrayList;
import java.util.List;

public class Traits {

    public final boolean cacheable;
    public final boolean deprecated;
    public final boolean experimental;
    public final boolean passthrough;
    public final boolean pure;
    public final boolean syncAware;

    Traits(Class<? extends Transformer<?, ?>> transformerClass) {
        cacheable = Transformers.supportsCaching(transformerClass);
        deprecated = transformerClass.getAnnotation(Deprecated.class) != null;
        experimental = transformerClass.getAnnotation(Experimental.class) != null;
        passthrough = transformerClass.getAnnotation(Passthrough.class) != null;
        pure = transformerClass.getAnnotation(Pure.class) != null;
        syncAware = Transformers.isSyncAware(transformerClass);
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();
        if (cacheable) list.add("cacheable");
        if (deprecated) list.add("deprecated");
        if (experimental) list.add("experimental");
        if (passthrough) list.add("passthrough");
        if (pure) list.add("pure");
        if (syncAware) list.add("sync-aware");
        return String.join(", ", list);
    }
}
