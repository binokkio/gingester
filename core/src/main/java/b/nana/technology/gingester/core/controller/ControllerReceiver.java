package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.receiver.Receiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

final class ControllerReceiver<I, O> implements Receiver<O> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gingester.class);

    private final Controller<I, O> controller;
    private final HashMap<Context, Integer> activeSyncs = new HashMap<>();

    ControllerReceiver(Controller<I, O> controller) {
        this.controller = controller;
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
        Context context = contextBuilder.build(controller);
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
        Context context = contextBuilder.build(controller);
        Controller<O, ?> target = controller.links.get(targetId);
        if (target == null) throw new IllegalStateException("Link not configured!");
        prepare(context);
        accept(context, output, target);
        finish(context);
    }

    private Context maybeExtend(Context context) {
        if (context.controller != controller && (
                !controller.syncs.isEmpty() ||
                !controller.excepts.isEmpty()
        )) {
            return context.extend().build(controller);
        } else {
            return context;
        }
    }

    private void prepare(Context context) {
        for (Controller<?, ?> target : controller.syncs) {
            target.prepare(context);
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
        if (!controller.syncs.isEmpty()) {
            startSync(context);
            if (Thread.currentThread() instanceof Worker) {
                ((Worker) Thread.currentThread()).flush();
            }
            for (Controller<?, ?> target : controller.indicates) {
                target.finish(controller, context);
            }
        }
    }

    private void startSync(Context context) {
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

        for (Context c : context) {
            if (!c.controller.excepts.isEmpty()) {
                for (Controller<Exception, ?> target : c.controller.excepts.values()) {
                    accept(context, cause, target);
                }
                return;
            } else if (c.controller.isExceptionHandler) {  // TODO this only works as long as a controller is not used as both a normal link and an exception handler
                break;
            }
        }

        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(String.format(
                    "Uncaught exception during %s::%s for %s",
                    context.fetch("transformer").findFirst().orElseThrow(),
                    context.fetch("method").findFirst().orElseThrow(),
                    context.fetchReverse("description").map(Object::toString).collect(Collectors.joining(" :: "))
            ), cause);
        }
    }
}
