package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.controller.SetupControls;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.zip.GZIPOutputStream;

public class Pack implements Transformer<byte[], Object> {

    private final ContextMap<TarArchiveOutputStream> contextMap = new ContextMap<>();
    private final Context.Template entryTemplate;
    private final Compressor compressor;
    private final String link;
    private final String passthrough;

    public Pack(Parameters parameters) {
        entryTemplate = Context.newTemplate(parameters.entry);
        compressor = getCompressor(parameters.compression);
        link = parameters.link;
        passthrough = parameters.passthrough;
    }

    private Compressor getCompressor(String compression) {
        switch (compression) {

            case "bz2":
                return BZip2CompressorOutputStream::new;

            case "gz":
            case "gzip":
                return GZIPOutputStream::new;

            case "xz":
                return XZCompressorOutputStream::new;

            default:
                throw new IllegalArgumentException("No case for " + compression);
        }
    }

    @Override
    public void setup(SetupControls controls) {
        controls.maxWorkers = 1;
        controls.requireAsync = true;
        controls.requireDownstreamAsync = true;

        if (passthrough != null) {
            if (link == null) throw new IllegalStateException("Given `passthrough` but not `link`");
            controls.links.add(link);
            controls.links.add(passthrough);
        }
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream();
        pipedOutputStream.connect(pipedInputStream);
        TarArchiveOutputStream tar = new TarArchiveOutputStream(compressor.wrap(pipedOutputStream));
        contextMap.put(context, () -> tar);
        if (link != null) {
            out.accept(context, pipedInputStream, link);
        } else {
            out.accept(context, pipedInputStream);
        }
    }

    @Override
    public void transform(Context context, byte[] in, Receiver<Object> out) throws Exception {
        TarArchiveOutputStream tar = contextMap.get(context);
        TarArchiveEntry entry = new TarArchiveEntry(entryTemplate.render(context));
        entry.setSize(in.length);
        tar.putArchiveEntry(entry);
        tar.write(in);
        tar.closeArchiveEntry();
        if (passthrough != null) out.accept(context, in, passthrough);
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        contextMap.remove(context).findFirst().orElseThrow().close();
    }

    public static class Parameters {

        public String entry;
        public String compression = "gz";
        public String link;
        public String passthrough;

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
