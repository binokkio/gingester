package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
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
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class Unpack implements Transformer<InputStream, InputStream> {

    private final Context.Template descriptionTemplate;

    public Unpack(Parameters parameters) {
        descriptionTemplate = Context.newTemplate(parameters.description);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws IOException {
        unpack(context, in, out, new ArrayDeque<>(Collections.singleton(descriptionTemplate.render(context))));
    }

    private void unpack(Context context, InputStream in, Receiver<InputStream> out, Deque<String> descriptions) throws IOException {

        String tailLowerCase = descriptions.getLast().toLowerCase(Locale.ENGLISH);

        if (tailLowerCase.endsWith(".bz2")) {
            Deque<String> copy = new ArrayDeque<>(descriptions);
            copy.add(trim(descriptions.getLast(), 4));
            unpack(context, new BZip2CompressorInputStream(in), out, copy);
        } else if (tailLowerCase.endsWith(".gz")) {
            Deque<String> copy = new ArrayDeque<>(descriptions);
            copy.add(trim(descriptions.getLast(), 3));
            unpack(context, new GZIPInputStream(in), out, copy);
        } else if (tailLowerCase.endsWith(".xz")) {
            Deque<String> copy = new ArrayDeque<>(descriptions);
            copy.add(trim(descriptions.getLast(), 3));
            unpack(context, new XZCompressorInputStream(in), out, copy);
        } else if (tailLowerCase.endsWith(".tar")) {
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(in);
            TarArchiveEntry tarArchiveEntry;
            while ((tarArchiveEntry = tarArchiveInputStream.getNextTarEntry()) != null) {
                if (tarArchiveEntry.isFile()) {
                    Deque<String> copy = new ArrayDeque<>(descriptions);
                    copy.add(tarArchiveEntry.getName());
                    unpack(context, tarArchiveInputStream, out, copy);
                }
            }
        } else if (tailLowerCase.endsWith(".zip")) {
            ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(in);
            ZipArchiveEntry zipArchiveEntry;
            while ((zipArchiveEntry = zipArchiveInputStream.getNextZipEntry()) != null) {
                if (!zipArchiveEntry.isDirectory()) {
                    Deque<String> copy = new ArrayDeque<>(descriptions);
                    copy.add(zipArchiveEntry.getName());
                    unpack(context, zipArchiveInputStream, out, copy);
                }
            }
        } else {
            out.accept(context.stash("description", descriptions.stream().skip(1).collect(Collectors.joining(" :: "))), in);
        }
    }

    private String trim(String input, int trim) {
        return input.substring(input.lastIndexOf('/') + 1, input.length() - trim);
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
