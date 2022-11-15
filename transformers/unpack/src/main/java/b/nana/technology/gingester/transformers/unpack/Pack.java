package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

@Names(1)
public final class Pack implements Transformer<byte[], OutputStreamWrapper> {

    private final ContextMap<TarArchiveOutputStream> contextMap = new ContextMap<>();
    private final Template entryTemplate;

    public Pack(Parameters parameters) {
        entryTemplate = Context.newTemplate(parameters.entry);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void prepare(Context context, Receiver<OutputStreamWrapper> out) {
        OutputStreamWrapper outputStreamWrapper = new OutputStreamWrapper();
        out.accept(context, outputStreamWrapper);
        TarArchiveOutputStream tar = new TarArchiveOutputStream(outputStreamWrapper);
        contextMap.put(context, tar);
    }

    @Override
    public void transform(Context context, byte[] in, Receiver<OutputStreamWrapper> out) throws Exception {
        TarArchiveEntry entry = new TarArchiveEntry(entryTemplate.render(context, in));
        entry.setSize(in.length);
        contextMap.act(context, tar -> {
            tar.putArchiveEntry(entry);
            tar.write(in);
            tar.closeArchiveEntry();
        });
    }

    @Override
    public void finish(Context context, Receiver<OutputStreamWrapper> out) throws Exception {
        contextMap.remove(context).close();
    }

    public static class Parameters {

        public TemplateParameters entry;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters entry) {
            this.entry = entry;
        }
    }
}
