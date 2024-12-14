package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class SplitList implements Transformer<String, List<String>> {

    private final String delimiter;
    private final Integer limit;

    public SplitList(Parameters parameters) {
        delimiter = parameters.delimiter;
        limit = parameters.limit;
    }

    @Override
    public void transform(Context context, String in, Receiver<List<String>> out) {
        if (limit == null)
            out.accept(context, Arrays.asList(in.split(Pattern.quote(delimiter))));
        else
            out.accept(context, Arrays.asList(in.split(Pattern.quote(delimiter), limit)));
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"delimiter", "limit"})
    public static class Parameters {
        public String delimiter;
        public Integer limit;
    }
}
