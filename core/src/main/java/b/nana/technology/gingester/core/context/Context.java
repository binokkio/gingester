package b.nana.technology.gingester.core.context;

import b.nana.technology.gingester.core.controller.Controller;

public class Context {

    public static final Context SEED = new Context();

    public Controller<?, ?> controller;  // TODO final

    private Context() {

    }
}
