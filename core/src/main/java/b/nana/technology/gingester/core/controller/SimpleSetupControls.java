package b.nana.technology.gingester.core.controller;

import java.util.ArrayList;
import java.util.List;

public final class SimpleSetupControls implements SetupControls {

    boolean requireAsync;
    final List<String> links = new ArrayList<>();

    @Override
    public void requireAsync() {
        requireAsync = true;
    }

    @Override
    public void link(String id) {
        links.add(id);
    }
}
