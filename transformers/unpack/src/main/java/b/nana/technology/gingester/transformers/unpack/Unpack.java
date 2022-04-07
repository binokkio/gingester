package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Names(1)
@Description("Recursively unpack an archive")
@Example(example = "", description = "Unpack input based on extensions found in stashed `description`")
@Example(example = "'data.tar.gz'", description = "Unpack input as a gzip compressed tar")
public final class Unpack implements Transformer<InputStream, InputStream> {

    private final Template descriptionTemplate;

    public Unpack(Parameters parameters) {
        descriptionTemplate = Context.newTemplate(parameters.description);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {
        unpack(context, in, out, new ArrayDeque<>(Collections.singleton(descriptionTemplate.render(context))));
    }

    private void unpack(Context context, InputStream in, Receiver<InputStream> out, Deque<String> descriptions) throws Exception {

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
        } else if (tailLowerCase.endsWith(".7z")) {
            byte[] bytes = in.readAllBytes();  // not ideal, should add an alternative Unpack7Z transformer
            SevenZFile sevenZFile = new SevenZFile(new SeekableInMemoryByteChannel(bytes));
            SevenZArchiveEntry sevenZArchiveEntry;
            while ((sevenZArchiveEntry = sevenZFile.getNextEntry()) != null) {
                if (!sevenZArchiveEntry.isDirectory()) {
                    Deque<String> copy = new ArrayDeque<>(descriptions);
                    copy.add(sevenZArchiveEntry.getName());
                    unpack(context, sevenZFile.getInputStream(sevenZArchiveEntry), out, copy);
                }
            }
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
        } else if (tailLowerCase.endsWith(".tgz")) {
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new GZIPInputStream(in));
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
        } else if (tailLowerCase.endsWith(".rar")) {
            Archive rarArchive = new Archive(in);
            for (FileHeader rarArchiveEntry : rarArchive) {
                if (!rarArchiveEntry.isDirectory()) {
                    Deque<String> copy = new ArrayDeque<>(descriptions);
                    copy.add(rarArchiveEntry.getFileName());
                    unpack(context, rarArchive.getInputStream(rarArchiveEntry), out, copy);
                }
            }
        } else {
            out.accept(
                    context.stash("description", descriptions.stream().skip(1).collect(Collectors.joining(" :: "))),
                    new NoCloseInputStream(in)
            );
        }
    }

    private String trim(String input, int trim) {
        return input.substring(input.lastIndexOf('/') + 1, input.length() - trim);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, text -> o("description", text));
            }
        }

        public TemplateParameters description = new TemplateParameters("${description}", false);
    }

    private static class NoCloseInputStream extends FilterInputStream {

        protected NoCloseInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
            // ignore
        }
    }
}
