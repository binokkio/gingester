package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

@Names(1)
@Passthrough
@Description("Route items based on their ordinality within the sync context, cycling the given weighted links")
@Example(example = "2 A 3 B", description = "Route the first 2 items to A, the next 3 to B, the next 2 to A, and so forth")
public final class RatioRoute extends OrdinalRoute{

    public RatioRoute(Parameters parameters) {
        super(parameters, true);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters extends OrdinalRoute.Parameters {
        public static class Deserializer extends StdDeserializer<Parameters> {

            protected Deserializer() {
                super(Parameters.class);
            }

            @Override
            public Parameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {

                ObjectCodec objectCodec = jsonParser.getCodec();
                JsonNode json = objectCodec.readTree(jsonParser);
                Parameters parameters = new Parameters();

                for (int i = 0; i < json.size(); ) {
                    int weight = json.get(i++).asInt();
                    String route = json.get(i++).asText();
                    for (int j = 0; j < weight; j++) {
                        parameters.add(route);
                    }
                }

                return parameters;
            }
        }
    }
}
