package b.nana.technology.gingester.transformers.base.transformers.socket;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.net.Socket;

public class Server extends Transformer<Void, Socket> {

    @Override
    protected void transform(Context context, Void input) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
