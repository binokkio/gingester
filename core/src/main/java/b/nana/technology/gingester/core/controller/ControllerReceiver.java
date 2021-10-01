package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.util.Map;

final class ControllerReceiver<I, O> implements Receiver<O> {

    private final Controller<I, O> controller;

    ControllerReceiver(Controller<I, O> controller) {
        this.controller = controller;
    }

    @Override
    public void accept(Context context, O output) {
        context = maybeExtend(context);
        prepare(context);
        for (Controller<O, ?> controller : controller.links.values()) {
            accept(context, output, controller);
        }
        finish(context);
    }

    @Override
    public void accept(Context.Builder contextBuilder, O output) {
        Context context = contextBuilder.build(controller);
        prepare(context);
        for (Controller<O, ?> controller : controller.links.values()) {
            accept(context, output, controller);
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
        for (Controller<?, ?> sync : controller.syncs) {
            sync.prepare(context);
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
            ((Worker) Thread.currentThread()).flush();
            for (Controller<?, ?> controller : controller.outgoing.values()) {
                controller.finish(controller, context);
            }
        }
    }

    public void except(String method, Context context, Exception cause) {
        except(context, cause, Map.of("method", method));
    }

    public void except(String method, Context context, I in, Exception cause) {
        except(context, cause, Map.of(
                "transformer", controller.id,
                "method", method,
                "stash", in
        ));
    }

    private void except(Context context, Exception cause, Map<String, Object> stash) {
        for (Context c : context) {
            if (c.isException()) break;  // don't bubble past previous exceptions to prevent exception handling loops
            if (!c.controller.excepts.isEmpty()) {
                for (Controller<Exception, ?> except : c.controller.excepts.values()) {
                    accept(context.stash(stash).exception().build(controller), cause, except);
                }
                return;
            }
        }
        cause.printStackTrace();
    }
}
