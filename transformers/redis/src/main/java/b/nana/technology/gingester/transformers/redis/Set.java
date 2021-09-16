package b.nana.technology.gingester.transformers.redis;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;

public class Set extends KeyTransformer<byte[], Void> {

    public Set(KeyTransformer.Parameters parameters) {
        super(parameters);
    }

    @Override
    protected void transform(Context context, byte[] in, Receiver<Void> out, Jedis jedis) {
        jedis.set(getKeyFormat().render(context).getBytes(StandardCharsets.UTF_8), in);
    }
}
