package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import java.io.*;
import java.util.zip.GZIPOutputStream;

@Names(1)
public final class Compress implements Transformer<InputStream, InputStream> {

    private final Compressor compressor;

    public Compress(Parameters parameters) {
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
        controls.requireOutgoingAsync();
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {

        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream();
        pipedOutputStream.connect(pipedInputStream);
        out.accept(context, pipedInputStream);

        OutputStream outputStream = compressor.wrap(pipedOutputStream);
        in.transferTo(outputStream);
        outputStream.close();
    }

    public static class Parameters {

        public String compression = "gz";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String compression) {
            this.compression = compression;
        }
    }

    private interface Compressor {
        OutputStream wrap(OutputStream outputStream) throws IOException;
    }
}
