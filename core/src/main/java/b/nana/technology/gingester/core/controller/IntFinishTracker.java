package b.nana.technology.gingester.core.controller;

final class IntFinishTracker implements FinishTracker {

    private final int indicatedTarget;
    private final int acknowledgedTarget;
    private int indicated;
    private int acknowledged;

    IntFinishTracker(Controller<?, ?> tracker, Context context) {
        indicatedTarget = tracker.syncedThrough.get(context.controller).size();
        acknowledgedTarget = ~0 >>> (32 - tracker.workers.size());
    }

    @Override
    public boolean indicate(Controller<?, ?> indicator) {
        return ++indicated == indicatedTarget;
    }

    @Override
    public boolean isFullyIndicated() {
        return indicated > indicatedTarget;
    }

    @Override
    public boolean acknowledge(Worker worker) {
        int before = acknowledged;
        acknowledged |= worker.mask;
        return before != acknowledged && acknowledged == acknowledgedTarget;
    }
}
