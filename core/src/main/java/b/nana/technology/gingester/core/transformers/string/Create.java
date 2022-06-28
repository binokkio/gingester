package b.nana.technology.gingester.core.transformers.string;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Description("Create a string, optionally using Freemarker templating")
@Example(example = "'Hello, World!'", description = "Create a simple string")
@Example(example = "'Hello, ${target}!'", description = "Create a string greeting a `target` stashed by an upstream transformer")
@Example(example = "'{template: \"path/to/template.txt\", is: \"FILE\"}'", description = "Read template from file", test = false)
@Example(example = "'{template: \"path/to/template.txt\", is: \"RESOURCE\"}'", description = "Read template from Java resource", test = false)
public final class Create implements Transformer<Object, String> {

    // TODO rename Def

    private final Template template;

    public Create(Parameters parameters) {
        template = Context.newTemplate(parameters.template);
    }

    @Override
    public void transform(Context context, Object in, Receiver<String> out) throws InterruptedException {
        out.accept(context, template.render(context));
    }

    public static class Parameters {

        public TemplateParameters template;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters template) {
            this.template = template;
        }
    }
}
