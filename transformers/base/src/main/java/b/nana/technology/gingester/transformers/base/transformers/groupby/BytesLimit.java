package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.ByteSizeParser;
import com.fasterxml.jackson.annotation.JsonCreator;

@Passthrough
public final class BytesLimit implements Transformer<byte[], byte[]> {

    private final ContextMap<State> contextMap = new ContextMap<>();
    private final long limit;

    public BytesLimit(Parameters parameters) {
        limit = ByteSizeParser.parse(parameters.limit);
    }

    @Override
    public void prepare(Context context, Receiver<byte[]> out) {
        contextMap.put(context, new State(context, out));
    }

    @Override
    public void transform(Context context, byte[] in, Receiver<byte[]> out) throws Exception {
        contextMap.act(context, state -> out.accept(state.group(context, in), in));
    }

    @Override
    public void finish(Context context, Receiver<byte[]> out) {
        contextMap.remove(context).closeCurrentGroup();
    }

    private class State {

        private final Context groupParent;
        private final Receiver<byte[]> out;

        private Context currentGroup;
        private long currentBytesCount = limit + 1;  // initialize currentBytesCount to limit + 1, guaranteeing a new group for the first bytes
        private long chunkCounter;

        public State(Context groupParent, Receiver<byte[]> out) {
            this.groupParent = groupParent;
            this.out = out;
        }

        private Context.Builder group(Context context, byte[] bytes) {

            currentBytesCount += bytes.length;

            if (currentBytesCount > limit) {
                closeCurrentGroup();
                currentGroup = out.acceptGroup(groupParent.stash("chunk", chunkCounter++));
                currentBytesCount = bytes.length;
            }

            return context.group(currentGroup);
        }

        private void closeCurrentGroup() {
            if (currentGroup != null) {
                out.closeGroup(currentGroup);
            }
        }
    }

    public static class Parameters {

        public String limit;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String limit) {
            this.limit = limit;
        }
    }
}
