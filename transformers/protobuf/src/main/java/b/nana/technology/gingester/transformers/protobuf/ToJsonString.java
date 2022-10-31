package b.nana.technology.gingester.transformers.protobuf;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

@Pure
public final class ToJsonString extends ToJsonBase implements Transformer<Message, String> {

    public ToJsonString(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, Message in, Receiver<String> out) throws InvalidProtocolBufferException {
        out.accept(context, getPrinter().print(in));
    }
}
