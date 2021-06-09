package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.base.common.FormatBase;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Format extends FormatBase<Path> {

    private final boolean stashMimeType;

    public Format(Parameters parameters) {
        super(parameters);
        stashMimeType = parameters.stashMimeType;
    }

    @Override
    protected void transform(Context context, Object input) throws Exception {

        String string = getFormat().format(context);
        Path path = Path.of(string);

        Context.Builder contextBuilder = context.extend(this)
                .description(string);

        if (stashMimeType) {
            String mimeType = Files.probeContentType(path);
            contextBuilder.stash(Map.of("mimeType", mimeType));
        }

        emit(contextBuilder, path);
    }

    public static class Parameters extends FormatBase.Parameters {

        public boolean stashMimeType = true;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String format) {
            super(format);
        }
    }
}
