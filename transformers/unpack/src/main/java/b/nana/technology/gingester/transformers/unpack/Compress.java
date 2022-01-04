package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.zip.GZIPOutputStream;

@Names(1)
public final class Compress implements Transformer<OutputStreamWrapper, OutputStreamWrapper> {

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
        controls.requireOutgoingSync();
    }

    @Override
    public void transform(Context context, OutputStreamWrapper in, Receiver<OutputStreamWrapper> out) throws Exception {
        OutputStreamWrapper next = new OutputStreamWrapper();
        OutputStream compressed = compressor.wrap(next);
        in.wrap(compressed);
        out.accept(context, next);
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
