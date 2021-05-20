package b.nana.technology.gingester.transformers.redis.transformers.redis;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.redis.common.KeyTransformer;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;

public class Publish extends KeyTransformer<byte[], Void> {

    public Publish(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected void transform(Context context, byte[] input, Jedis jedis) {
        jedis.publish(getKeyFormat().format(context).getBytes(StandardCharsets.UTF_8), input);
    }
}
