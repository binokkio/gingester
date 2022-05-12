package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Names(1)
public final class Merge implements Transformer<Object, Object> {

    private final ContextMap<Map<String, List<Object>>> state = new ContextMap<>();
    private final Parameters parameters;

    public Merge(Parameters parameters) {
        this.parameters = parameters;
        for (Instruction instruction : parameters) {
            requireNonNull(instruction.fetch);
            if (instruction.fetch.isOrdinal()) {
                requireNonNull(instruction.stash);
            } else if (instruction.stash == null) {
                instruction.stash = instruction.fetch.toString();
            }
        }
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        Map<String, List<Object>> merged = new HashMap<>();
        for (Instruction instruction : parameters) {
            merged.put(instruction.stash, new ArrayList<>());
        }
        state.put(context, merged);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        state.act(context, merged -> {
            for (Instruction instruction : parameters) {
                context.fetch(instruction.fetch).findFirst().ifPresent(
                        object -> merged.get(instruction.stash).add(object));
            }
        });
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {

        Map<String, List<Object>> merged = state.remove(context);
        Map<String, Object> stash = new HashMap<>(merged.size());

        for (Instruction instruction : parameters) {
            List<Object> values = merged.get(instruction.stash);
            if (instruction.list) {
                stash.put(instruction.stash, values);
            } else {
                if (values.size() == 1) {
                    stash.put(instruction.stash, values.get(0));
                } else if (!values.isEmpty()){
                    throw new IllegalStateException("Multiple values for " + instruction.stash);
                }
            }
        }

        out.accept(context.stash(stash), "merge signal");
    }

    public static class Parameters extends ArrayList<Instruction> {

    }

    public static class Instruction {

        public FetchKey fetch;
        public String stash;
        public boolean list;

        @JsonCreator
        public Instruction() {}

        @JsonCreator
        public Instruction(FetchKey fetch) {
            this.fetch = fetch;
        }
    }
}
