package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.regex.Matcher;

public final class Group implements Transformer<Matcher, String> {

    private final int group;
    private final String groupName;

    public Group(Parameters parameters) {

        if (parameters.group != null && parameters.groupName != null)
            throw new IllegalArgumentException("Both group and groupName given");

        group = parameters.group != null ? parameters.group : -1;
        groupName = parameters.groupName;
    }

    @Override
    public void transform(Context context, Matcher in, Receiver<String> out) {
        if (groupName != null) {
            out.accept(context, in.group(groupName));
        } else {
            out.accept(context, in.group(group));
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, i -> o("group", i));
                rule(JsonNode::isTextual, i -> o("groupName", i));
            }
        }

        public Integer group;
        public String groupName;
    }
}
