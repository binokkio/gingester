package b.nana.technology.gingester.transformers.redis;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;

public final class Subscribe extends KeyTransformer<Object, byte[]> {

    public Subscribe(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected void transform(Context context, Object in, Receiver<byte[]> out, Jedis jedis) {
        jedis.subscribe(new BinaryJedisPubSub() {
            @Override
            public void onMessage(byte[] channel, byte[] message) {
                out.accept(context.stash("description", new String(channel)), message);
            }
        }, getKeyTemplate().render(context).getBytes(StandardCharsets.UTF_8));
    }
}
