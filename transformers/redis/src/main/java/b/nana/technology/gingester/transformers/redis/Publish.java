package b.nana.technology.gingester.transformers.redis;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;

public final class Publish extends KeyTransformer<byte[], byte[]> {

    public Publish(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected void transform(Context context, byte[] in, Receiver<byte[]> out, Jedis jedis) {
        jedis.publish(getKeyTemplate().render(context, in).getBytes(StandardCharsets.UTF_8), in);
        out.accept(context, in);
    }
}
