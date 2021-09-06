package b.nana.technology.gingester.transformers.base.transformers.resource;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.controller.SetupControls;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;
import java.util.Map;

public class Open implements Transformer<Object, InputStream> {

    private final Context.Template pathTemplate;

    public Open(Parameters parameters) {
        pathTemplate = Context.newTemplate(parameters.path);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireDownstreamSync = true;
    }

    @Override
    public void transform(Context context, Object in, Receiver<InputStream> out) throws Exception {
        String resourcePath = pathTemplate.render(context);
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) throw new NullPointerException("getResourceAsStream(\"" + resourcePath + "\") returned null");
        out.accept(context.stash("description", resourcePath), inputStream);
        inputStream.close();
    }

    public static class Parameters {

        public String path;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String path) {
            this.path = path;
        }
    }
}
