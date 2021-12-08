package b.nana.technology.gingester.core.controller;

public interface FinishTracker {

    static FinishTracker newInstance(Controller<?, ?> controller, Context context) {
        return controller.workers.size() >= 32 ?
                new SetFinishTracker(controller, context) :
                new IntFinishTracker(controller, context);
    }

    boolean indicate(Controller<?, ?> indicator);
    boolean acknowledge(Worker worker);
    boolean isFullyIndicated();
}
