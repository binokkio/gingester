package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Passthrough
public final class SetHeaders implements Transformer<Object, Object> {

    private final FetchKey fetchHttpResponse = new FetchKey("http.response");

    private final Map<String, Template> headers;

    public SetHeaders(Parameters parameters) {
        headers = parameters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Context.newTemplate(e.getValue())));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        HttpResponse response = (HttpResponse) context.fetch(fetchHttpResponse)
                .orElseThrow(() -> new IllegalStateException("Context did not come from HttpServer"));

        headers.forEach((name, valueTemplate) -> response.addHeader(name, valueTemplate.render(context, in)));

        out.accept(context, in);
    }

    public static class Parameters extends HashMap<String, TemplateParameters> {

    }
}
