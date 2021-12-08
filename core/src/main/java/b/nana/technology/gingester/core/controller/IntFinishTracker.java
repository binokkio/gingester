package b.nana.technology.gingester.core.controller;

final class IntFinishTracker implements FinishTracker {

    private final Controller<?, ?> tracker;
    private final Context context;
    private final int indicatedTarget;
    private int indicated;
    private int acknowledged;

    IntFinishTracker(Controller<?, ?> tracker, Context context) {
        this.tracker = tracker;
        this.context = context;
        this.indicatedTarget = tracker.syncedThrough.get(context.controller).size();
        this.acknowledged = ~0 >>> (32 - tracker.workers.size());
    }

    @Override
    public boolean indicate(Controller<?, ?> indicator) {
        return ++indicated == indicatedTarget;
    }

    @Override
    public boolean acknowledge(Worker worker) {
        acknowledged ^= 1 << worker.id;
        return acknowledged == 0;
    }

    @Override
    public boolean isFullyIndicated() {
        return indicated > indicatedTarget;
    }
}
