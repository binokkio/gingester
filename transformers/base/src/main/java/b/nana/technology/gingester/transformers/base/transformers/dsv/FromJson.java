package b.nana.technology.gingester.transformers.base.transformers.dsv;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class FromJson implements Transformer<JsonNode, OutputStreamWrapper> {

    private final ObjectWriter writer;

    private final ContextMap<State> states = new ContextMap<>();

    public FromJson(Parameters parameters) {
        CsvMapper csvMapper = new CsvMapper()
                .enable(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING);
        CsvSchema csvSchema = CsvSchema.emptySchema()
                .withoutHeader()
                .withColumnSeparator(parameters.delimiter)
                .withQuoteChar(parameters.quote);
        writer = csvMapper.writer().with(csvSchema);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void prepare(Context context, Receiver<OutputStreamWrapper> out) {
        OutputStreamWrapper outputStreamWrapper = new OutputStreamWrapper();
        out.accept(context, outputStreamWrapper);
        states.put(context, new State(writer, outputStreamWrapper));
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<OutputStreamWrapper> out) throws Exception {
        states.act(context, state -> {

            if (!state.headerWritten) {
                List<String> header = new ArrayList<>(in.size());
                in.fieldNames().forEachRemaining(header::add);
                state.writer.write(header);
                state.headerWritten = true;
            }

            List<String> row = new ArrayList<>(in.size());
            in.iterator().forEachRemaining(jsonNode -> row.add(jsonNode.textValue()));
            state.writer.write(row);
        });
    }

    @Override
    public void finish(Context context, Receiver<OutputStreamWrapper> out) throws Exception {
        states.remove(context).writer.close();
    }

    private static class State {

        private final SequenceWriter writer;
        private boolean headerWritten;

        public State(ObjectWriter writer, OutputStreamWrapper outputStreamWrapper) {
            try {
                this.writer = writer.writeValues(outputStreamWrapper);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Parameters {
        public char delimiter = ',';
        public char quote = '"';
    }
}
