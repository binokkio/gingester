package b.nana.technology.gingester.transformers.base.common.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;

public abstract class IteratingToJsonTransformer<T> extends ReadingToJsonTransformer<T> {

    protected IteratingToJsonTransformer(Parameters parameters) {
        super(parameters);
    }

    @Override
    public final void transform(Context context, T in, Receiver<JsonNode> out) throws Exception {
        JsonParser parser = getJsonParser(getObjectReader(), in);
        JsonToken token = parser.nextToken();
        if (token != null) {
            MappingIterator<JsonNode> iterator = getObjectReader().readValues(parser);
            long counter = 0;
            while (iterator.hasNext()) {
                out.accept(context.stash("description", counter++), iterator.next());
            }
        }
    }

    protected abstract JsonParser getJsonParser(ObjectReader objectReader, T in) throws IOException;
}
