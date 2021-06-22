package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.ContextMap;
import b.nana.technology.gingester.core.Transformer;
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
import java.util.List;

public class ToDsvInputStream extends Transformer<JsonNode, InputStream> {

    private final ObjectWriter writer;

    private final ContextMap<State> states = new ContextMap<>();

    public ToDsvInputStream(Parameters parameters) {
        CsvMapper csvMapper = new CsvMapper()
                .enable(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING);
        CsvSchema csvSchema = CsvSchema.emptySchema()
                .withoutHeader()
                .withColumnSeparator(parameters.delimiter)
                .withQuoteChar(parameters.quote);
        writer = csvMapper.writer().with(csvSchema);
    }

    @Override
    protected void setup(Setup setup) {
        setup.requireDownstreamAsync();
    }

    @Override
    protected void prepare(Context context) {
        State state = new State(writer);
        states.put(context, state);
        emit(context, state.pipedInputStream);
    }

    @Override
    protected void transform(Context context, JsonNode input) throws Exception {

        State state = states.require(context);

        if (!state.headerWritten) {
            List<String> header = new ArrayList<>(input.size());
            input.fieldNames().forEachRemaining(header::add);
            state.writer.write(header);
            state.headerWritten = true;
        }

        List<String> row = new ArrayList<>(input.size());
        input.iterator().forEachRemaining(jsonNode -> row.add(jsonNode.textValue()));
        state.writer.write(row);
    }

    @Override
    protected void finish(Context context) throws Exception {
        states.requireRemove(context).writer.close();
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
