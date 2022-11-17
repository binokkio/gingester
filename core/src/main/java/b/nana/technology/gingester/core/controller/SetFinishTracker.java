package b.nana.technology.gingester.core.controller;

import java.util.HashSet;
import java.util.Set;

final class SetFinishTracker implements FinishTracker {

    private final Controller<?, ?> tracker;
    private final Context context;
    private final Set<Controller<?, ?>> indicated = new HashSet<>();
    private final Set<Worker> acknowledged = new HashSet<>();

    SetFinishTracker(Controller<?, ?> tracker, Context context) {
        this.tracker = tracker;
        this.context = context;
    }

    @Override
    public boolean indicate(Controller<?, ?> indicator) {
        indicated.add(indicator);
        return indicated.size() == tracker.syncedThrough.get(context.controller).size();
    }

    @Override
    public boolean isFullyIndicated() {
        return indicated.contains(tracker);
    }

    @Override
    public boolean awaits(Worker worker) {
        return !acknowledged.contains(worker);
    }

    @Override
    public boolean acknowledge(Worker worker) {
        return acknowledged.add(worker) && acknowledged.size() == tracker.workers.size();
    }
}
