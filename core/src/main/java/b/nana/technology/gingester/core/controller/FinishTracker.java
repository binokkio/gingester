package b.nana.technology.gingester.core.controller;

import java.util.HashSet;
import java.util.Set;

final class FinishTracker {

    private final Controller<?, ?> tracker;
    private final Context context;
    private final Set<Controller<?, ?>> indicated = new HashSet<>();
    private final Set<Thread> acknowledged = new HashSet<>();

    FinishTracker(Controller<?, ?> tracker, Context context) {
        this.tracker = tracker;
        this.context = context;
    }

    boolean indicate(Controller<?, ?> indicator) {
        indicated.add(indicator);
        return isReadyForQueue();
    }

    boolean acknowledge(Thread thread) {
        acknowledged.add(thread);
        return acknowledged.size() == tracker.workers.size();
    }

    private boolean isReadyForQueue() {
        if (context.isSeed() && context.controller == tracker) {
            return true;
        } else {
            return indicated.size() == tracker.syncedThrough.get(context.controller).size();
        }
    }

    boolean isFullyIndicated() {
        return indicated.contains(tracker);
    }
}
