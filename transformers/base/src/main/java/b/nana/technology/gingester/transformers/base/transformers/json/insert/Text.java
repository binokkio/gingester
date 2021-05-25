package b.nana.technology.gingester.transformers.base.transformers.json.insert;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.base.common.json.insert.InsertBase;
import com.fasterxml.jackson.databind.JsonNode;

public class Text extends InsertBase {

    private final String[] jsonPath;
    private final Context.StringFormat textFormat;

    public Text(Parameters parameters) {
        super(parameters);
        jsonPath = parameters.jsonPath;
        textFormat = new Context.StringFormat(parameters.textFormat);
    }

    @Override
    protected void setup(Setup setup) {
        setup.preferUpstreamSync();
    }

    @Override
    protected void transform(Context context, JsonNode input) {
        prepare(input, jsonPath).put(jsonPath[jsonPath.length - 1], textFormat.format(context));
        emit(context, input);
    }

    public static class Parameters extends InsertBase.Parameters {
        public String[] jsonPath;
        public String textFormat;
    }
}
