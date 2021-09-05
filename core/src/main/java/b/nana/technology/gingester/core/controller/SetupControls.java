package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.reporting.Counter;

import java.util.ArrayList;

public final class SetupControls {
    public boolean requireDownstreamSync;
    public boolean requireDownstreamAsync;
    public final ArrayList<String> links = new ArrayList<>();
    public final ArrayList<String> syncs = new ArrayList<>();
    public Counter acksCounter;
}
