package b.nana.technology.gingester.transformers.redis;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;

public final class LPush extends KeyTransformer<byte[], byte[]> {

    public LPush(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected void transform(Context context, byte[] in, Receiver<byte[]> out, Jedis jedis) {
        jedis.lpush(getKeyTemplate().render(context).getBytes(StandardCharsets.UTF_8), in);
        out.accept(context, in);
    }
}
