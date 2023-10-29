package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Find implements Transformer<String, Matcher> {

    private final boolean stashNamedGroups;
    private final TemplateMapper<PatternWithNamedGroups> patternWith;

    public Find(Parameters parameters) {
        stashNamedGroups = parameters.stashNamedGroups;
        patternWith = Context.newTemplateMapper(parameters.regex, PatternWithNamedGroups::new);
    }

    @Override
    public void transform(Context context, String in, Receiver<Matcher> out) {
        PatternWithNamedGroups patternWith = this.patternWith.render(context, in);
        Matcher matcher = patternWith.pattern.matcher(in);
        while (matcher.find()) {
            if (stashNamedGroups) {
                Map<String, String> stash = new HashMap<>();
                for (String namedGroup : patternWith.namedGroups) {
                    String value = matcher.group(namedGroup);
                    if (value != null) stash.put(namedGroup, value);
                }
                out.accept(context.stash(stash), matcher);
            } else {
                out.accept(context, matcher);
            }
        }
    }

    private class PatternWithNamedGroups {

        private static final Pattern NAMED_GROUP_PATTERN = Pattern.compile("\\(\\?<(.*?)>");

        private final Pattern pattern;
        private final List<String> namedGroups;

        private PatternWithNamedGroups(String pattern) {

            this.pattern = Pattern.compile(pattern);

            if (stashNamedGroups) {
                this.namedGroups = new ArrayList<>();
                Matcher matcher = NAMED_GROUP_PATTERN.matcher(pattern);
                while (matcher.find()) namedGroups.add(matcher.group(1));
            } else {
                namedGroups = null;
            }
        }
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order("regex")
    public static class Parameters {
        public TemplateParameters regex;
        public boolean stashNamedGroups = true;
    }
}
