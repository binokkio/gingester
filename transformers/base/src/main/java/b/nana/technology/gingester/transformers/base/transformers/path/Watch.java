package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class Watch implements Transformer<Object, Path> {

    private final FileSystem fileSystem = FileSystems.getDefault();
    private final List<TemplateMapper<Path>> targets;

    public Watch(Parameters parameters) {
        targets = parameters.targets.stream().map(tp -> Context.newTemplateMapper(tp, Paths::get)).collect(Collectors.toList());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Path> out) throws Exception {

        List<Path> targets = this.targets.stream().map(t -> t.render(context, in)).collect(Collectors.toList());
        Set<Path> register = new HashSet<>();

        for (Path target : targets) {
            if (Files.isRegularFile(target)) {
                register.add(target.getParent());
            } else if (Files.isDirectory(target)) {
                register.add(target);
            } else {
                throw new IllegalArgumentException("Handling for non-file non-directory paths not implemented, path: " + target);
            }
        }

        ArrayBlockingQueue<Path> queue = new ArrayBlockingQueue<>(100);

        register.forEach(path -> new Thread(() -> {
            try {
                WatchService watchService = fileSystem.newWatchService();

                path.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                );

                while (true) {

                    WatchKey key = watchService.poll(10, TimeUnit.SECONDS);
                    if (key == null)
                        continue;

                    for (WatchEvent<?> event : key.pollEvents())
                        queue.put(((Path) key.watchable()).resolve((Path) event.context()));

                    if (!key.reset())
                        throw new UnsupportedOperationException("Handling of invalid keys after reset not implemented");
                }

            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start());

        while (true) {
            Path path = queue.take();
            if (targets.stream().anyMatch(path::startsWith)) {
                out.accept(context, path);
            }
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, target -> o("targets", a(target)));
                rule(JsonNode::isArray, targets -> o("targets", targets));
            }
        }

        public List<TemplateParameters> targets;
    }
}
