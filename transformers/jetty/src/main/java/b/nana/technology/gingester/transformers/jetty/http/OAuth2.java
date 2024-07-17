package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Passthrough
public final class OAuth2 implements Transformer<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final URI tokenUrl;
    private final String tokenRequestBody;

    private String accessToken;
    private Instant expiresAt = Instant.now().minusSeconds(1);
    private Duration margin = Duration.ofMinutes(1);

    public OAuth2(Parameters parameters) {

        tokenUrl = URI.create(parameters.tokenUrl);

        StringBuilder tokenRequestBodyBuilder = new StringBuilder()
                .append("client_id=").append(requireNonNull(parameters.clientId, "Missing clientId parameter"))
                .append("&client_secret=").append(requireNonNull(parameters.clientSecret, "Missing clientSecret parameter"))
                .append("&grant_type=client_credentials");

        if (!parameters.scopes.isEmpty())
            tokenRequestBodyBuilder.append("&scope=").append(String.join("%20", parameters.scopes));

        tokenRequestBody = tokenRequestBodyBuilder.toString();
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        if (expiresAt.minus(margin).isBefore(Instant.now())) {

            LOGGER.info("Requesting new access token");

            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder(tokenUrl);
            requestBuilder.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(tokenRequestBody));

            JsonNode response;
            try (InputStream inputStream = HTTP_CLIENT.send(requestBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofInputStream()).body()) {
                response = OBJECT_MAPPER.readTree(inputStream);
            }

            JsonNode expiresInNode = response.path("expires_in");
            if (expiresInNode.isMissingNode())
                throw new IllegalStateException("Response did not include \"expires_in\"");

            JsonNode accessTokenNode = response.path("access_token");
            if (accessTokenNode.isMissingNode())
                throw new IllegalStateException("Response did not include \"access_token\"");

            expiresAt = Instant.now().plusSeconds(expiresInNode.asInt());
            accessToken = accessTokenNode.asText();
        }

        out.accept(context.stash("accessToken", accessToken), in);
    }

    public static class Parameters {
        public String clientId;
        public String clientSecret;
        public List<String> scopes = List.of();
        public String tokenUrl;
    }
}
