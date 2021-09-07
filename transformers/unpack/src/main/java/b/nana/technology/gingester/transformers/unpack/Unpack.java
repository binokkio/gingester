package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.SetupControls;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public class Unpack implements Transformer<InputStream, InputStream> {

    private final Context.Template descriptionTemplate;

    public Unpack(Parameters parameters) {
        descriptionTemplate = Context.newTemplate(parameters.description);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireDownstreamSync = true;
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws IOException {

        String description = descriptionTemplate.render(context);
        description = description.substring(description.lastIndexOf('/') + 1);
        String descriptionLowerCase = description.toLowerCase(Locale.ENGLISH);

        if (descriptionLowerCase.endsWith(".bz2")) {
            transform(
                    out.build(context.stash("description", trimEnd(description, 4))),
                    new BZip2CompressorInputStream(in), out
            );
        } else if (descriptionLowerCase.endsWith(".gz")) {
            transform(
                    out.build(context.stash("description", trimEnd(description, 3))),
                    new GZIPInputStream(in), out
            );
        } else if (descriptionLowerCase.endsWith(".xz")) {
            transform(
                    out.build(context.stash("description", trimEnd(description, 3))),
                    new XZCompressorInputStream(in), out
            );
        } else if (descriptionLowerCase.endsWith(".tar")) {
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(in);
            TarArchiveEntry tarArchiveEntry;
            while ((tarArchiveEntry = tarArchiveInputStream.getNextTarEntry()) != null) {
                if (tarArchiveEntry.isFile()) {
                    transform(
                            out.build(context.stash("description", tarArchiveEntry.getName())),
                            tarArchiveInputStream, out
                    );
                }
            }
        } else if (descriptionLowerCase.endsWith(".zip")) {
            ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(in);
            ZipArchiveEntry zipArchiveEntry;
            while ((zipArchiveEntry = zipArchiveInputStream.getNextZipEntry()) != null) {
                if (!zipArchiveEntry.isDirectory()) {
                    transform(
                            out.build(context.stash("description", zipArchiveEntry.getName())),
                            zipArchiveInputStream, out
                    );
                }
            }
        } else {
            out.accept(context, in);
        }
    }

    private String trimEnd(String input, int trim) {
        return input.substring(0, input.length() - trim);
    }

    public static class Parameters {

        public String description = "${description}";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String description) {
            this.description = description;
        }
    }
}
