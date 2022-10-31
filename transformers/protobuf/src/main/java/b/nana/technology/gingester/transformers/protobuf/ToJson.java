package b.nana.technology.gingester.transformers.protobuf;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

@Pure
public final class ToJson extends ToJsonBase implements Transformer<Message, JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToJson(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, Message in, Receiver<JsonNode> out) throws InvalidProtocolBufferException, JsonProcessingException {
        String json = getPrinter().print(in);
        JsonNode jsonNode = objectMapper.readTree(json);
        out.accept(context, jsonNode);
    }
}
