package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Names(1)
public final class Throw implements Transformer<Object, Void> {

    private final Template messageTemplate;

    public Throw(Parameters parameters) {
        this.messageTemplate = Context.newTemplate(parameters.message);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Void> out) throws Exception {
        throw new FlowException(messageTemplate.render(context));
    }

    public static class FlowException extends Exception {
        public FlowException(String message) {
            super(message);
        }
    }

    public static class Parameters {

        public TemplateParameters message;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters message) {
            this.message = message;
        }
    }
}
