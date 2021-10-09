package b.nana.technology.gingester.transformers.base.transformers.dsv;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FromJson implements Transformer<JsonNode, InputStream> {

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
        controls.syncs(Collections.singletonList("__seed__"));
        controls.requireOutgoingAsync();
    }

    @Override
    public void prepare(Context context, Receiver<InputStream> out) {
        State state = new State(writer);
        states.put(context, state);
        out.accept(context, state.pipedInputStream);
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<InputStream> out) throws Exception {
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
    public void finish(Context context, Receiver<InputStream> out) throws Exception {
        states.remove(context).writer.close();
    }

    private static class State {

        private final SequenceWriter writer;
        private final PipedInputStream pipedInputStream;
        private boolean headerWritten;

        public State(ObjectWriter writer) {
            PipedOutputStream pipedOutputStream = new PipedOutputStream();
            pipedInputStream = new PipedInputStream();
            try {
                pipedOutputStream.connect(pipedInputStream);
                this.writer = writer.writeValues(pipedOutputStream);
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
