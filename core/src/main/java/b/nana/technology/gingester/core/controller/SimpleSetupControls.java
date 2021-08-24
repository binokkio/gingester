package b.nana.technology.gingester.core.controller;

import java.util.ArrayList;
import java.util.List;

public class SimpleSetupControls implements SetupControls {

    final List<String> links = new ArrayList<>();

    @Override
    public void link(String id) {
        links.add(id);
    }
}
