package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public final class AsBigDecimal implements Transformer<JsonNode, BigDecimal> {

    @Override
    public void transform(Context context, JsonNode in, Receiver<BigDecimal> out) throws Exception {
        out.accept(context, new BigDecimal(in.asText()));
    }
}