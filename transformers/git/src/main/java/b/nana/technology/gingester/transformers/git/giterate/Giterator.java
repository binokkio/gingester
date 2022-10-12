package b.nana.technology.gingester.transformers.git.giterate;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

interface Giterator {
    Map<String, Object> getStashDetails();
    void giterate(Path clone, List<Commit> commits, Context context, Receiver<Path> out) throws IOException, InterruptedException;
}
