package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class Unpack implements Transformer<InputStream, InputStream> {

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws IOException {

        String description = context.fetch("description")
                .map(o -> (String) o)
                .map(s -> s.substring(s.lastIndexOf('/') + 1))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unpack received context without description"));

        String descriptionLowerCase = description.toLowerCase(Locale.ENGLISH);

        if (descriptionLowerCase.endsWith(".gz")) {
            transform(
                    context.stash(Map.of("description", trimEnd(description, 3))).build(),
                    new GZIPInputStream(in),
                    out
            );
        } else if (descriptionLowerCase.endsWith(".tar")) {
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(in);
            TarArchiveEntry tarArchiveEntry;
            while ((tarArchiveEntry = tarArchiveInputStream.getNextTarEntry()) != null) {
                if (tarArchiveEntry.isFile()) {
                    transform(
                            context.stash(Map.of("description", tarArchiveEntry.getName())).build(),
                            tarArchiveInputStream,
                            out
                    );
                }
            }
        } else if (descriptionLowerCase.endsWith(".zip")) {
            ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(in);
            ZipArchiveEntry zipArchiveEntry;
            while ((zipArchiveEntry = zipArchiveInputStream.getNextZipEntry()) != null) {
                if (!zipArchiveEntry.isDirectory()) {
                    transform(
                            context.stash(Map.of("description", zipArchiveEntry.getName())).build(),
                            zipArchiveInputStream,
                            out
                    );
                }
            }
        } else if (descriptionLowerCase.endsWith(".bz2")) {
            transform(
                    context.stash(Map.of("description", trimEnd(description, 4))).build(),
                    new BZip2CompressorInputStream(in),
                    out
            );
        } else {
            out.accept(context, in);
        }
    }

    private String trimEnd(String input, int trim) {
        return input.substring(0, input.length() - trim);
    }
}
