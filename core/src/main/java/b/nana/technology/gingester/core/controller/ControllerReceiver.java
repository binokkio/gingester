package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.util.HashMap;
import java.util.Map;

final class ControllerReceiver<I, O> implements Receiver<O> {

    private final Controller<I, O> controller;
    private final HashMap<Context, Integer> activeSyncs = new HashMap<>();

    private boolean controllerHasSyncs;
    private boolean controllerHasSyncsOrExcepts;

    ControllerReceiver(Controller<I, O> controller) {
        this.controller = controller;
    }

    public void examineController() {
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
        Context context = contextBuilder.synced(controllerHasSyncs).build(controller);  // TODO synced only if builder has no group
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
        Context context = contextBuilder.synced(controllerHasSyncs).build(controller);  // TODO synced only if builder has no group
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
        if (context.controller != controller && controllerHasSyncsOrExcepts) {
            return context.extend().synced(controllerHasSyncs).build(controller);
        } else {
            return context;
        }
    }

    private void prepare(Context context) {
        if (context.isSynced() && !context.hasGroup()) {  // TODO prevent grouped context to have synced true instead
            for (Controller<?, ?> target : controller.syncs) {
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
        if (controllerHasSyncs && !context.hasGroup()) {
            startFinishSignal(context);
            if (Thread.currentThread() instanceof Worker) {
                ((Worker) Thread.currentThread()).flush();
            }
            for (Controller<?, ?> target : controller.indicates) {
                target.finish(controller, context);
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

    void onFinishSignalReachedLeave(Context context) {
        if (context.isSeed()) return;
        synchronized (activeSyncs) {
            int count = activeSyncs.remove(context);
            if (++count == controller.downstreamLeaves) {
                activeSyncs.notify();
            } else {
                activeSyncs.put(context, count);
            }
        }
    }

    public void except(String method, Context context, Exception cause) {
        except(context.stash(Map.of(
                "transformer", controller.id,
                "method", method,
                "exception", cause
        )).build(controller), cause);
    }

    public void except(String method, Context context, I in, Exception cause) {
        except(context.stash(Map.of(
                "transformer", controller.id,
                "method", method,
                "exception", cause,
                "stash", in
        )).build(controller), cause);
    }

    private void except(Context context, Exception cause) {

        boolean handled = false;

        for (Context c : context) {
            c.markFlawed();
            if (!handled) {
                if (!c.controller.excepts.isEmpty()) {
                    c.controller.excepts.values().forEach(target -> accept(context, cause, target));
                    handled = true;
                } else if (c.controller.isExceptionHandler) {
                    throw new IllegalStateException(c.controller.id + " is an exception handler without `excepts`");
                }
            }
        }

        if (!handled) {
            throw new IllegalStateException("No exception handlers for exception", cause);
        }
    }
}
