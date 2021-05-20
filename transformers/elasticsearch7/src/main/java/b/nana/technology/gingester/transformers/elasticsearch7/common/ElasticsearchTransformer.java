package b.nana.technology.gingester.transformers.elasticsearch7.common;

import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public abstract class ElasticsearchTransformer<I, O> extends Transformer<I, O> {

    protected final RestHighLevelClient restClient;

    public ElasticsearchTransformer(Parameters parameters) {

        super(parameters);

        restClient = new RestHighLevelClient(RestClient.builder(
                new HttpHost(parameters.host, parameters.port, parameters.schema)));
    }

    public static class Parameters {

        public String schema = "http";
        public String host = "localhost";
        public int port = 9200;

        @JsonCreator
        public Parameters() {}
    }
}
