package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Names(1)
public final class OnFinish implements Transformer<Object, Object> {

    private static final String FINISH_SIGNAL = "finish signal";

    private final List<String> flawless;
    private final List<String> flawed;

    public OnFinish(Parameters parameters) {

        flawless = parameters.flawless;
        flawed = parameters.flawed;

        if (flawless != null && flawed != null && (flawless.isEmpty() || flawed.isEmpty())) {
            throw new IllegalStateException("Explicit empty list not allowed for OnFinish parameter");
        }
    }

    @Override
    public void setup(SetupControls controls) {
        List<String> links = new ArrayList<>();
        if (flawless != null) links.addAll(flawless);
        if (flawed != null) links.addAll(flawed);
        if (!links.isEmpty()) controls.links(links);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

    }

    @Override
    public void finish(Context context, Receiver<Object> out) {
        if (flawless == null && flawed == null) {
            out.accept(context, FINISH_SIGNAL);
        } else if (context.isFlawless()) {
            if (flawless != null) {
                if (flawless.isEmpty()) {
                    out.accept(context, FINISH_SIGNAL);
                } else {
                    for (String target : flawless) {
                        out.accept(context, FINISH_SIGNAL, target);
                    }
                }
            }
        } else if (flawed != null) {
            if (flawed.isEmpty()) {
                out.accept(context, FINISH_SIGNAL);
            } else {
                for (String target : flawed) {
                    out.accept(context, FINISH_SIGNAL, target);
                }
            }
        }
    }

    public static class Parameters {

        public List<String> flawless;
        public List<String> flawed;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String string) {
            if (string.equals("flawless")) {
                flawless = Collections.emptyList();
            } else if (string.equals("flawed")) {
                flawed = Collections.emptyList();
            } else {
                throw new IllegalArgumentException("String argument for OnFinish must be either 'flawless' or 'flawed'");
            }
        }
    }
}
