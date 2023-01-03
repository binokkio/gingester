package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.common.ByteCountFormat;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class RespondPath implements Transformer<Path, String> {

    private final FetchKey fetchHttpResponse = new FetchKey("http.response");
    private final FetchKey fetchHttpRequestPath = new FetchKey("http.request.path");
    private final Template directoryTemplate;
    private final boolean redirdir;

    public RespondPath(Parameters parameters) {
        directoryTemplate = Context.newTemplate(parameters.directoryTemplate);
        redirdir = parameters.redirdir;
    }

    @Override
    public void transform(Context context, Path in, Receiver<String> out) throws Exception {

        HttpResponse response = (HttpResponse) context.fetch(fetchHttpResponse)
                .orElseThrow(() -> new IllegalStateException("Context did not come from HttpServer"));

        if (!Files.exists(in)) {

            response.setStatus(404);
            response.getOutputStream().write("404 Not Found".getBytes(StandardCharsets.UTF_8));

        } else if (Files.isDirectory(in)) {

            Optional<Object> optionalRequestPath = context.fetch(fetchHttpRequestPath);

            if (redirdir && optionalRequestPath.filter(path -> !((String) path).endsWith("/")).isPresent()) {
                response.setStatus(301);
                response.addHeader("Location", optionalRequestPath.get() + "/");
            } else {

                Map<String, String> entries = new TreeMap<>();

                if (optionalRequestPath.filter(path -> !path.equals("/")).isPresent())
                    entries.put("..", "parent directory");

                Files.walkFileTree(in, new FileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

                        if (dir.equals(in))
                            return FileVisitResult.CONTINUE;

                        entries.put(dir.getFileName() + "/", "directory");

                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        entries.put(file.getFileName().toString(), new ByteCountFormat().format(attrs.size()));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path path, IOException e) {
                        // TODO
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path path, IOException e) {
                        return FileVisitResult.CONTINUE;
                    }
                });
                String render = directoryTemplate.render(context, in, Map.of("entries", entries));
                response.getOutputStream().write(render.getBytes(StandardCharsets.UTF_8));
            }

        } else {

            String contentTypeDetectionInput = in.getFileName().toString();
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(contentTypeDetectionInput);

            if (contentType != null) {
                response.addHeader("Content-Type", contentType);
            }

            response.addHeader("Content-Length", Long.toString(Files.size(in)));

            try (InputStream inputStream = Files.newInputStream(in)) {
                inputStream.transferTo(response.getOutputStream());
            }
        }

        response.finish();
        out.accept(context, "http respond signal");
    }

    public static class Parameters {
        public TemplateParameters directoryTemplate = new TemplateParameters("/http/directory.html", TemplateParameters.Interpretation.RESOURCE, false);
        public boolean redirdir = true;
    }
}
