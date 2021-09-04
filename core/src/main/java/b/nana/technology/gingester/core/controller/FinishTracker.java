package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.context.Context;

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
        return isFullyAcknowledged();
    }

    private boolean isReadyForQueue() {
        if (context.isSeed()) {
            return indicated.size() >= tracker.incoming.size();
        } else {
            return indicated.size() == tracker.syncedThrough.get(context.controller).size();  // untested
        }
    }

    boolean isFullyIndicated() {
        return indicated.contains(tracker);
    }

    private boolean isFullyAcknowledged() {
        return acknowledged.size() == tracker.workers.size();
    }
}