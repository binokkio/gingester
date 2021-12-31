package b.nana.technology.gingester.core.controller;

public interface FinishTracker {

    static FinishTracker newInstance(Controller<?, ?> controller, Context context) {
        return controller.workers.size() >= 32 ?
                new SetFinishTracker(controller, context) :
                new IntFinishTracker(controller, context);
    }

    boolean indicate(Controller<?, ?> indicator);
    boolean isFullyIndicated();

    /**
     * Acknowledge finish signal.
     *
     * @param worker the worker who is acknowledging
     * @return true if all workers have acknowledged and the given worker did so for the first time
     */
    boolean acknowledge(Worker worker);
}
