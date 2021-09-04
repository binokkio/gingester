package b.nana.technology.gingester.core.context;

import b.nana.technology.gingester.core.controller.Controller;

import java.util.Collections;
import java.util.Map;

public class Context {

    public static Context newSeed(Controller<?, ?> seedController) {
        return new Context(seedController);
    }


    private final Context parent;
    public final Controller<?, ?> controller;
    private final Map<String, Object> stash;

    private Context(Controller<?, ?> seedController) {
        parent = null;
        controller = seedController;
        stash = Collections.emptyMap();
    }

    private Context(Builder builder) {
        parent = builder.parent;
        controller = builder.controller;
        stash = builder.stash;
    }

    public boolean isSeed() {
        return parent == null;
    }

    public Builder extend() {
        return new Builder(this);
    }

    public static class Builder {

        private final Context parent;
        private Controller<?, ?> controller;
        private Map<String, Object> stash;

        private Builder(Context parent) {
            this.parent = parent;
        }

        public Builder controller(Controller<?, ?> controller) {
            this.controller = controller;
            return this;
        }

        public Builder stash(Map<String, Object> stash) {
            this.stash = stash;
            return this;
        }

        public Context build() {
            return new Context(this);
        }
    }
}
