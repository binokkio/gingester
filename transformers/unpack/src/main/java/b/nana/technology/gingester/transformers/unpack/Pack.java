package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import java.io.*;
import java.util.Collections;
import java.util.zip.GZIPOutputStream;

@Names(1)
public final class Pack implements Transformer<byte[], InputStream> {

    private final ContextMap<TarArchiveOutputStream> contextMap = new ContextMap<>();
    private final Context.Template entryTemplate;
    private final Compressor compressor;

    public Pack(Parameters parameters) {
        entryTemplate = Context.newTemplate(parameters.entry);
        compressor = getCompressor(parameters.compression);
    }

    private Compressor getCompressor(String compression) {
        switch (compression) {

            case "bz2":
                return BZip2CompressorOutputStream::new;

            case "gz":
            case "gzip":
                return GZIPOutputStream::new;

            case "none":
                return outputStream -> outputStream;

            case "xz":
                return XZCompressorOutputStream::new;

            default:
                throw new IllegalArgumentException("No case for " + compression);
        }
    }

    @Override
    public void setup(SetupControls controls) {
        controls.syncs(Collections.singletonList("__seed__"));
        controls.requireOutgoingAsync();
    }

    @Override
    public void prepare(Context context, Receiver<InputStream> out) throws Exception {
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream();
        pipedOutputStream.connect(pipedInputStream);
        TarArchiveOutputStream tar = new TarArchiveOutputStream(compressor.wrap(pipedOutputStream));
        contextMap.put(context, tar);
        out.accept(context, pipedInputStream);
    }

    @Override
    public void transform(Context context, byte[] in, Receiver<InputStream> out) throws Exception {
        TarArchiveEntry entry = new TarArchiveEntry(entryTemplate.render(context));
        entry.setSize(in.length);
        contextMap.act(context, tar -> {
            tar.putArchiveEntry(entry);
            tar.write(in);
            tar.closeArchiveEntry();
        });
    }

    @Override
    public void finish(Context context, Receiver<InputStream> out) throws Exception {
        contextMap.remove(context).close();
    }

    public static class Parameters {

        public String entry;
        public String compression = "gz";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String entry) {
            this.entry = entry;
        }
    }

    private interface Compressor {
        OutputStream wrap(OutputStream outputStream) throws IOException;
    }
}
