package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.context.Context;

import java.util.HashSet;
import java.util.Set;

final class FinishTracker {

    final Controller<?, ?> trackedBy;
    final Context context;
    final Set<Controller<?, ?>> indicated = new HashSet<>();
    final Set<Thread> acknowledged = new HashSet<>();

    FinishTracker(Controller<?, ?> trackedBy, Context context) {
        this.trackedBy = trackedBy;
        this.context = context;
    }

    boolean isFullyIndicated() {
        if (context == Context.SEED) {
            return indicated.size() >= trackedBy.incoming.size();
        } else {
            return indicated.size() == trackedBy.syncedThrough.get(context.controller).size();
        }
    }

    boolean isFullyAcknowledged() {
        return acknowledged.size() == trackedBy.workers.size();
    }
}
