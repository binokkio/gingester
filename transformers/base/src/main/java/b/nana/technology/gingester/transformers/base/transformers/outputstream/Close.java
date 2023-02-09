package b.nana.technology.gingester.transformers.base.transformers.outputstream;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.OutputStream;

@Passthrough
@Names(1)
public final class Close implements Transformer<Object, Object> {

    private final FetchKey fetchTarget;

    public Close(Parameters parameters) {
        fetchTarget = parameters.target;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        ((OutputStream) context.require(fetchTarget)).close();
        out.accept(context, in);
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "target" })
    public static class Parameters {
        public FetchKey target;
    }
}
