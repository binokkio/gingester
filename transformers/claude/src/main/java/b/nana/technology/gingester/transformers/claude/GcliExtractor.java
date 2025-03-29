package b.nana.technology.gingester.transformers.claude;

import b.nana.technology.gingester.core.annotations.Experimental;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;

@Experimental
public final class GcliExtractor implements Transformer<JsonNode, String> {

    @Override
    public void transform(Context context, JsonNode in, Receiver<String> out) {

        // create a set of str_replace_editor usages
        Set<String> strReplaceEditorUsages = new HashSet<>();
        for (JsonNode message : in) {
            if (message.has("content")) {
                for (JsonNode content : message.get("content")) {
                    if (content.get("type").asText().equals("tool_use") && content.get("name").asText().equals("str_replace_editor")) {
                        strReplaceEditorUsages.add(content.get("id").asText());
                    }
                }
            }
        }

        // find the latest tool_result for any of the str_replace_editor usages
        for (int i = in.size() - 1; i >= 0; i--) {
            JsonNode message = in.get(i);
            JsonNode content = message.path("content");
            for (int j = content.size() - 1; j >= 0; j--) {
                JsonNode part = content.get(j);
                if (part.get("type").asText().equals("tool_result") && strReplaceEditorUsages.contains(part.get("tool_use_id").asText())) {
                    out.accept(context, part.get("content").asText());
                    return;
                }
            }
        }
    }
}
