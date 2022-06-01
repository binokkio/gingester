package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.annotations.Example;

@Example(example = "$.hello", description = "Remove and yield the JsonNode at key \"hello\", throw if missing")
@Example(example = "$.hello optional", description = "Remove and yield the JsonNode at key \"hello\", ignore if missing")
public final class Remove extends Path {

    public Remove(Parameters parameters) {
        super(parameters, true);
    }
}
