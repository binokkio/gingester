package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

final class ControllerReceiver<O> implements Receiver<O> {

    private final Controller<?, O> controller;

    ControllerReceiver(Controller<?, O> controller) {
        this.controller = controller;
    }

    @Override
    public void accept(Context context, O output) {
        context = maybeExtend(context);
        prepare(context);
        for (Controller<O, ?> controller : controller.outgoing.values()) {
            accept(context, output, controller);
        }
        finish(context);
    }

    @Override
    public void accept(Context.Builder contextBuilder, O output) {
        Context context = contextBuilder.controller(controller).build();
        prepare(context);
        for (Controller<O, ?> controller : controller.outgoing.values()) {
            accept(context, output, controller);
        }
        finish(context);
    }

    @Override
    public void accept(Context context, O output, String targetId) {
        context = maybeExtend(context);
        Controller<O, ?> target = controller.outgoing.get(targetId);
        if (target == null) throw new IllegalStateException("Link not configured!");
        prepare(context);
        accept(context, output, target);
        finish(context);
    }

    @Override
    public void accept(Context.Builder contextBuilder, O output, String targetId) {
        Context context = contextBuilder.controller(controller).build();
        Controller<O, ?> target = controller.outgoing.get(targetId);
        if (target == null) throw new IllegalStateException("Link not configured!");
        prepare(context);
        accept(context, output, target);
        finish(context);
    }

    private Context maybeExtend(Context context) {
        if (!controller.syncs.isEmpty() && context.controller != controller) {
            return context.extend().controller(controller).build();
        } else {
            return context;
        }
    }

    private void prepare(Context context) {
        for (Controller<?, ?> sync : controller.syncs) {
            sync.prepare(context);
        }
    }

    private void accept(Context context, O output, Controller<O, ?> target) {
        if (target.async) {
            Thread thread = Thread.currentThread();
            if (thread instanceof Worker) {
                ((Worker) thread).accept(context, output, target);
            } else {
                target.accept(new Batch<>(context, output));
            }
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
