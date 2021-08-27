package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

final class ControllerReceiver<O> implements Receiver<O> {

    private final Controller<?, O> controller;

    ControllerReceiver(Controller<?, O> controller) {
        this.controller = controller;
    }

    @Override
    public void accept(Context context, O output) {
        prepare(context);
        for (Controller<O, ?> controller : controller.outgoing.values()) {
            accept(context, output, controller);
        }
        finish(context);
    }

    @Override
    public void accept(Context context, O output, String targetId) {
        Controller<O, ?> target = controller.outgoing.get(targetId);
        if (target == null) throw new IllegalStateException("Link not configured!");
        prepare(context);
        accept(context, output, target);
        finish(context);
    }

    private void prepare(Context context) {
        for (Controller<?, ?> sync : controller.syncs) {
            sync.prepare(context);
        }
    }

    private void accept(Context context, O output, Controller<O, ?> target) {
        if (target.async) {
            throw new UnsupportedOperationException("Not yet implemented");
        } else {
            target.transform(context, output);
        }
    }

    private void finish(Context context) {
        if (!controller.syncs.isEmpty()) {
            for (Controller<O, ?> controller : controller.outgoing.values()) {
                controller.finish(controller, context);
            }
        }
    }
}
