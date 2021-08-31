package b.nana.technology.gingester.core.controller;

import java.util.HashSet;
import java.util.Set;

final class FinishTracker {
    final Set<Controller<?, ?>> indicated = new HashSet<>();
    final Set<Thread> acknowledged = new HashSet<>();
}
