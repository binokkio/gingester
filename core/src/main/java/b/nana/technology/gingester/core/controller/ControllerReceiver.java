package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.FlowRunner;
import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.configuration.ControllerConfiguration;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class ControllerReceiver<I, O> implements Receiver<O> {

    private final Controller<I, O> controller;
    private final Controller<O, ?> soleLink;
    private final HashMap<Context, Integer> activeSyncs = new HashMap<>();
    private final int controllerSyncs;
    private final boolean controllerHasSyncs;
    private final boolean controllerHasSyncsOrExcepts;
    private final boolean debugMode;

    ControllerReceiver(Controller<I, O> controller, ControllerConfiguration<I, O> configuration, FlowRunner.ControllerInterface controllerInterface) {
        this.controller = controller;
        this.soleLink = controller.links.size() == 1 ? controller.links.values().iterator().next() : null;
        this.controllerSyncs = (int) controllerInterface.getConfigurations().stream()
                .filter(c -> c.getSyncs().contains(controller.id))
                .count();
        this.controllerHasSyncs = controllerSyncs != 0;
        this.controllerHasSyncsOrExcepts = controllerHasSyncs || !configuration.getExcepts().isEmpty();
        this.debugMode = controllerInterface.isDebugModeEnabled();
    }

    @Override
    public void accept(Context context, O output) {
        context = maybeExtend(context);
        prepare(context);
        if (soleLink != null) accept(context, output, soleLink);
        else for (Controller<O, ?> target : controller.links.values())
            accept(context, output, target);
        finish(context);
    }

    @Override
    public void accept(Context.Builder contextBuilder, O output) {
        Context context = contextBuilder.synced(!contextBuilder.hasGroup() && controllerHasSyncs).build(controller);
        prepare(context);
        if (soleLink != null) accept(context, output, soleLink);
        else for (Controller<O, ?> target : controller.links.values())
            accept(context, output, target);
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
                Worker worker = null;
                if (Thread.currentThread() instanceof Worker) {
                    worker = ((Worker) Thread.currentThread());
                    worker.flush();
                }
                for (Controller<?, ?> target : targets) {
                    target.finish(controller, context, worker);
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

    void onFinishSignalReachedSyncTo(Context context) {
        if (context.isSeed()) return;
        synchronized (activeSyncs) {
            if (controllerSyncs == 1) {
                activeSyncs.remove(context);
                activeSyncs.notify();
            } else if (activeSyncs.computeIfPresent(context, this::countFinishSignalReachedSyncTo) == null) {
                activeSyncs.notify();
            }
        }
    }

    private Integer countFinishSignalReachedSyncTo(Context context, int count) {
        // increment until we reach `controllerSyncs`, then return null so the context-count mapping
        // gets removed from `activeSyncs`
        return ++count == controllerSyncs ? null : count;
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
