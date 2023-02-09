package b.nana.technology.gingester.transformers.jetty.http.mock;

import b.nana.technology.gingester.transformers.jetty.http.HttpRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class MockRequest {

    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> query;
    private final String remoteIp;
    private final InputStream requestBody;

    public MockRequest(Builder builder) {
        method = builder.method;
        path = builder.path;
        headers = builder.headers;
        query = builder.query;
        remoteIp = builder.remoteIp;
        requestBody = builder.requestBody != null ? builder.requestBody : new ByteArrayInputStream(new byte[0]);
    }

    public Map<String, Object> getStash() {
        return Map.of(
                "method", method,
                "path", path,
                "headers", headers,
                "query", query,
                "object", (HttpRequest) () -> remoteIp
        );
    }

    public InputStream getRequestBody() {
        return requestBody;
    }

    public static class Builder {

        private String method;
        private String path;
        private Map<String, String> headers = new HashMap<>();
        private Map<String, String> query = new HashMap<>();
        private String remoteIp = "127.0.0.1";
        private InputStream requestBody;

        public Builder() {

        }

        public Builder(String methodAndPath) {
            String[] parts = methodAndPath.split(" ", 2);
            method = parts[0];
            path = parts[1];
        }

        public Builder copy() {

            if (requestBody != null)
                throw new IllegalStateException("Can't copy MockRequest.Builder that has a requestBody");

            Builder builder = new Builder();
            builder.method = method;
            builder.path = path;
            builder.headers = new HashMap<>(headers);
            builder.query = new HashMap<>(query);
            builder.remoteIp = remoteIp;
            return builder;
        }

        public Builder setMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder addHeader(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public Builder addQueryParameter(String key, String value) {
            query.put(key, value);
            return this;
        }

        public Builder setRequestBody(String requestBody) {
            requireNonNull(requestBody, "requestBody must not be null");
            this.requestBody = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        public Builder setRequestBody(byte[] requestBody) {
            requireNonNull(requestBody, "requestBody must not be null");
            this.requestBody = new ByteArrayInputStream(requestBody);
            return this;
        }

        public Builder setRequestBody(InputStream requestBody) {
            requireNonNull(requestBody, "requestBody must not be null");
            this.requestBody = requestBody;
            return this;
        }

        public MockRequest build() {
            return new MockRequest(this);
        }
    }
}
