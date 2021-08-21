package b.nana.technology.gingester.transformers.elasticsearch7.common;

import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public abstract class ElasticsearchTransformer<I, O> extends Transformer<I, O> {

    private final HttpHost[] hosts;
    private final String authorization;

    protected RestHighLevelClient restClient;

    public ElasticsearchTransformer(Parameters parameters) {
        super(parameters);

        hosts = new HttpHost[parameters.hosts.size()];
        for (int i = 0; i < parameters.hosts.size(); i++) {
            hosts[i] = new HttpHost(
                    parameters.hosts.get(i).host,
                    parameters.hosts.get(i).port,
                    parameters.hosts.get(i).schema
            );
        }

        authorization = parameters.username != null ?
                Base64.getEncoder().encodeToString((parameters.username + ':' + parameters.password).getBytes(StandardCharsets.UTF_8)) :
                null;
    }

    @Override
    protected void open() {

        RestClientBuilder builder = RestClient.builder(hosts);

        if (authorization != null) {
            builder.setDefaultHeaders(new Header[]{new BasicHeader(
                    "Authorization",
                    authorization
            )});
        }

        restClient = new RestHighLevelClient(builder);
    }

    @Override
    protected void close() throws Exception {
        restClient.close();
    }

    public static class Parameters {

        public List<Host> hosts = Collections.singletonList(new Host());
        public String username;
        public String password;

        @JsonCreator
        public Parameters() {}

        public static class Host {
            public String schema = "http";
            public String host = "localhost";
            public int port = 9200;
        }
    }
}
