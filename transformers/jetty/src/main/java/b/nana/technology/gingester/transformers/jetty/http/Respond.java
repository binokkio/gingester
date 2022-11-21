package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.InputStream;
import java.net.URLConnection;

public final class Respond implements Transformer<InputStream, String> {

    private final FetchKey fetchHttpResponse = new FetchKey("http.response");
    private final Template contentTypeDetectionInputTemplate;

    public Respond(Parameters parameters) {
        contentTypeDetectionInputTemplate = Context.newTemplate(parameters.detectContentTypeFrom);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<String> out) throws Exception {

        Server.ResponseWrapper response = (Server.ResponseWrapper) context.fetch(fetchHttpResponse)
                .orElseThrow(() -> new IllegalStateException("Context did not come from HttpServer"));

        if (!response.hasHeader("Content-Type")) {

            String contentTypeDetectionInput = contentTypeDetectionInputTemplate.render(context, in);
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(contentTypeDetectionInput);

            if (contentType != null) {
                response.addHeader("Content-Type", contentType);
            }
        }

        response.respond(servlet -> in.transferTo(servlet.getOutputStream()));

        out.accept(context, "http respond signal");
    }

    public static class Parameters {
        public TemplateParameters detectContentTypeFrom = new TemplateParameters("${description}", false);
    }
}
