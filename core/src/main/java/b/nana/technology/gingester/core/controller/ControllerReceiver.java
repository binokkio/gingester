package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class ControllerReceiver<I, O> implements Receiver<O> {

    private final Controller<I, O> controller;
    private final HashMap<Context, Integer> activeSyncs = new HashMap<>();
    private final boolean debugMode;

    private boolean controllerHasSyncs;
    private boolean controllerHasSyncsOrExcepts;

    ControllerReceiver(Controller<I, O> controller, boolean debugMode) {
        this.controller = controller;
        this.debugMode = debugMode;
    }

    void examineController() {
        controllerHasSyncs = !controller.syncs.isEmpty();
        controllerHasSyncsOrExcepts =
                controllerHasSyncs ||
                !controller.excepts.isEmpty();
    }

    @Override
    public void accept(Context context, O output) {
        context = maybeExtend(context);
        prepare(context);
        for (Controller<O, ?> target : controller.links.values()) {
            accept(context, output, target);
        }
        finish(context);
    }

    @Override
    public void accept(Context.Builder contextBuilder, O output) {
        Context context = contextBuilder.synced(!contextBuilder.hasGroup() && controllerHasSyncs).build(controller);
        prepare(context);
        for (Controller<O, ?> target : controller.links.values()) {
            accept(context, output, target);
        }
        finish(context);
    }

    @Override
    public void accept(Context context, O output, String targetId) {
        context = maybeExtend(context);
        Controller<O, ?> target = controller.links.get(targetId);
        if (target == null) throw new IllegalStateException("Link not configured!");
        prepare(context);
        accept(context, output, target);
        finish(context);
    }

    @Override
    public void accept(Context.Builder contextBuilder, O output, String targetId) {
        Context context = contextBuilder.synced(!contextBuilder.hasGroup() && controllerHasSyncs).build(controller);
        Controller<O, ?> target = controller.links.get(targetId);
        if (target == null) throw new IllegalStateException("Link not configured!");
        prepare(context);
        accept(context, output, target);
        finish(context);
    }

    @Override
    public Context acceptGroup(Context.Builder group) {
        Context context = group.synced(true).build(controller);
        prepare(context);
        return context;
    }

    @Override
    public void closeGroup(Context group) {
        finish(group);
    }

    private Context maybeExtend(Context context) {
        if (context.controller != controller && (controllerHasSyncsOrExcepts || debugMode)) {
            return context.extend().synced(controllerHasSyncs).build(controller);
        } else {
            return context;
        }
    }

    private void prepare(Context context) {
        if (context.controller == controller && context.isSynced()) {
            for (Controller<?, ?> target : controller.syncs) {  // TODO reverse?
                target.prepare(context);
            }
        }
    }

    private <T> void accept(Context context, T in, Controller<T, ?> target) {
        if (target.async) {
            Thread thread = Thread.currentThread();
            if (thread instanceof Worker) {
                ((Worker) thread).accept(context, in, target);
            } else {
                target.accept(new Batch<>(context, in));
            }
        } else {
            target.transform(context, in);
        }
    }

    private void finish(Context context) {
        if (context.controller == controller && context.isSynced()) {
            Set<Controller<?, ?>> targets = controller.indicates.get(controller);
            if (targets != null) {
                startFinishSignal(context);
                if (Thread.currentThread() instanceof Worker) {
                    ((Worker) Thread.currentThread()).flush();
                }
                for (Controller<?, ?> target : targets) {
                    target.finish(controller, context);
                }
            }
        }
    }

    private void startFinishSignal(Context context) {
        synchronized (activeSyncs) {
            while (activeSyncs.size() >= controller.maxQueueSize) {
                try {
                    activeSyncs.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);  // TODO
                }
            }
            activeSyncs.put(context, 0);
        }
    }

    void onFinishSignalReachedTarget(Context context) {
        if (context.isSeed()) return;
        synchronized (activeSyncs) {
            int count = activeSyncs.remove(context);
            if (++count == controller.syncs.size()) {
                activeSyncs.notify();
            } else {
                activeSyncs.put(context, count);
            }
        }
    }

    void except(String method, Context context, Exception cause) {
        Controller<?, ?> catcher = except(context, cause);
        Context next = context.stash(Map.of(
                "transformer", controller.id.toString(),
                "method", method,
                "exception", cause,
                "caughtBy", catcher.id.toString()
        )).build(controller);
        catcher.excepts.values().forEach(target -> accept(next, cause, target));
    }

    void except(String method, Context context, I in, Exception cause) {
        Controller<?, ?> catcher = except(context, cause);
        Context next = context.stash(Map.of(
                "transformer", controller.id.toString(),
                "method", method,
                "in", in,
                "exception", cause,
                "caughtBy", catcher.id.toString()
        )).build(controller);
        catcher.excepts.values().forEach(target -> accept(next, cause, target));
    }

    private Controller<?, ?> except(Context context, Exception cause) {
        if (!controller.excepts.isEmpty()) {
            return controller;
        } else {
            for (Context c : context) {
                c.markFlawed();
                if (!c.controller.excepts.isEmpty()) {
                    return c.controller;
                } else if (c.controller.isExceptionHandler) {
                    throw new IllegalStateException(c.controller.id + " is an exception handler with empty `excepts`");
                }
            }
        }
        throw new IllegalStateException("No exception handlers for exception", cause);
    }
}
