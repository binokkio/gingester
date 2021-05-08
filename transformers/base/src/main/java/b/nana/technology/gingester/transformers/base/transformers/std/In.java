package b.nana.technology.gingester.transformers.base.transformers.std;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;

public class In extends Transformer<Void, InputStream> {

    @Override
    protected void transform(Context context, Void input) throws Exception {
        emit(context.extend(this).description("stdin"), System.in);
    }
}
