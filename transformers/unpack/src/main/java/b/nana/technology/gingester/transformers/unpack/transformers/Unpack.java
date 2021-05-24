package b.nana.technology.gingester.transformers.unpack.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.InputStream;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public class Unpack extends Transformer<InputStream, InputStream> {

    @Override
    protected void setup(Setup setup) {
        setup.requireDownstreamSync();
    }

    @Override
    protected void transform(Context context, InputStream input) throws Exception {

        String descriptionTail = context.getDescriptionTail()
                .orElseThrow(() -> new IllegalStateException("Unpack received context without description"));

        String descriptionTailLower = descriptionTail.toLowerCase(Locale.ENGLISH);

        if (descriptionTailLower.endsWith(".gz")) {
            recurse(
                    context.extend(this).description(trimEnd(descriptionTail, 3)),
                    new GZIPInputStream(input)
            );
        } else if (descriptionTailLower.endsWith(".tar")) {
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(input);
            TarArchiveEntry tarArchiveEntry;
            while ((tarArchiveEntry = tarArchiveInputStream.getNextTarEntry()) != null) {
                recurse(
                        context.extend(this).description(tarArchiveEntry.getName()),
                        tarArchiveInputStream
                );
            }
        } else if (descriptionTailLower.endsWith(".zip")) {
            ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(input);
            ZipArchiveEntry zipArchiveEntry;
            while ((zipArchiveEntry = zipArchiveInputStream.getNextZipEntry()) != null) {
                recurse(
                        context.extend(this).description(zipArchiveEntry.getName()),
                        zipArchiveInputStream
                );
            }
        } else if (descriptionTailLower.endsWith(".bz2")) {
            recurse(
                    context.extend(this).description(trimEnd(descriptionTail, 3)),
                    new BZip2CompressorInputStream(input)
            );
        } else {
            emit(context, input);
        }
    }

    private String trimEnd(String input, int trim) {
        return input.substring(0, input.length() - trim);
    }
}
