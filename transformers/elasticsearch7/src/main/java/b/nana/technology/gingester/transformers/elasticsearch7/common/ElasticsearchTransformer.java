package b.nana.technology.gingester.transformers.elasticsearch7.common;

import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public abstract class ElasticsearchTransformer<I, O> extends Transformer<I, O> {

    private final String host;
    private final int port;
    private final String schema;

    protected RestHighLevelClient restClient;

    public ElasticsearchTransformer(Parameters parameters) {
        super(parameters);
        host = parameters.host;
        port = parameters.port;
        schema = parameters.schema;
    }

    @Override
    protected void open() {
        restClient = new RestHighLevelClient(RestClient.builder(
                new HttpHost(host, port, schema)));
    }

    @Override
    protected void close() throws Exception {
        restClient.close();
    }

    public static class Parameters {

        public String schema = "http";
        public String host = "localhost";
        public int port = 9200;

        @JsonCreator
        public Parameters() {}
    }
}
