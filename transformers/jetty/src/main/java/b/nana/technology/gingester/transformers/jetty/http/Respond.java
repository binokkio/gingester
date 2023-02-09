package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.net.URLConnection;
import java.util.Map;
import java.util.stream.Collectors;

public final class Respond implements Transformer<InputStream, String> {

    private final int statusCode;
    private final Map<String, Template> headers;
    private final FetchKey fetchHttpResponse = new FetchKey("http.response");
    private final Template contentTypeDetectionInputTemplate;

    public Respond(Parameters parameters) {
        statusCode = parameters.statusCode;
        headers = parameters.headers.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> Context.newTemplate(e.getValue())
        ));
        contentTypeDetectionInputTemplate = Context.newTemplate(parameters.detectContentTypeFrom);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<String> out) throws Exception {

        HttpResponse response = (HttpResponse) context.fetch(fetchHttpResponse)
                .orElseThrow(() -> new IllegalStateException("Context did not come from HttpServer"));

        if (statusCode != 0)
            response.setStatus(statusCode);

        headers.forEach((name, template) ->
                response.addHeader(name, template.render(context, in)));

        if (!response.hasHeader("Content-Type")) {

            String contentTypeDetectionInput = contentTypeDetectionInputTemplate.render(context, in);
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(contentTypeDetectionInput);

            if (contentType != null) {
                response.addHeader("Content-Type", contentType);
            }
        }

        in.transferTo(response.getOutputStream());
        response.finish();

        out.accept(context, "http respond signal");
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, NormalizingDeserializer::a);
                rule(JsonNode::isObject, o -> o.has("statusCode") || o.has("headers") || o.has("detectContentTypeFrom") ? o : a(o));
                rule(JsonNode::isArray, a -> {
                    ObjectNode result = JsonNodeFactory.instance.objectNode();
                    for (int i = 0; i < a.size(); i++) {
                        JsonNode entry = a.get(i);
                        if (entry.isInt()) result.set("statusCode", entry);
                        else if (entry.isObject()) result.set("headers", entry);
                        else throw new IllegalArgumentException("Unexpected argument: " + entry);
                    }
                    return result;
                });
            }
        }

        public int statusCode;
        public Map<String, TemplateParameters> headers = Map.of();
        public TemplateParameters detectContentTypeFrom = new TemplateParameters("${description!''}", false);
    }
}
