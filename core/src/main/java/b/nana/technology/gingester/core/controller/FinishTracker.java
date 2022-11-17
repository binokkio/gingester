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
     * Check if this FinishTracker is awaiting acknowledgment from a worker.
     *
     * @param worker the worker for which to check
     * @return true if the given worker has now acknowledged this FinishTracker
     */
    boolean awaits(Worker worker);

    /**
     * Acknowledge finish signal.
     *
     * @param worker the worker who is acknowledging
     * @return true if all workers have acknowledged and the given worker did so for the first time
     */
    boolean acknowledge(Worker worker);
}
