package b.nana.technology.gingester.transformers.hadoop.transformers.hdfs;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.IOException;
import java.io.InputStream;

public final class Search implements Transformer<Object, InputStream> {

    private final String hdfs;
    private final String root;

    public Search(Parameters parameters) {
        hdfs = parameters.hdfs;
        root = parameters.root;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void transform(Context context, Object in, Receiver<InputStream> out) throws IOException {

        Configuration configuration = new Configuration();
        configuration.set("fs.defaultFS", hdfs);

        FileSystem fileSystem = FileSystem.get(configuration);

        RemoteIterator<LocatedFileStatus> remoteIterator = fileSystem.listFiles(new Path(root), true);
        while (remoteIterator.hasNext()) {
            Path file = remoteIterator.next().getPath();
            try (InputStream inputStream = fileSystem.open(file)) {
                out.accept(
                        context.stash("description", file.toString()),
                        inputStream
                );
            }
        }
    }

    public static class Parameters {

        public String hdfs = "hdfs://localhost:9000";
        public String root;

        @JsonCreator
        public Parameters() {

        }

        @JsonCreator
        public Parameters(String root) {
            this.root = root;
        }
    }
}
