package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.transformers.base.common.FromStashBase;
import b.nana.technology.gingester.transformers.base.transformers.Stash;
import com.fasterxml.jackson.databind.JsonNode;

public class FromStash extends FromStashBase<JsonNode> {
    public FromStash(Stash.Parameters parameters) {
        super(parameters, JsonNode.class);
    }
}
