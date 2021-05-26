package b.nana.technology.gingester.transformers.elasticsearch7.transformers.elasticsearch;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.elasticsearch7.common.ElasticsearchTransformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Write extends ElasticsearchTransformer<byte[], Void> implements BulkProcessor.Listener {

    private final Context.StringFormat indexFormat;
    private final Context.StringFormat idFormat;
    private final String mapping;
    private final Set<String> indicesSeen = new HashSet<>();
    private BulkProcessor bulkProcessor;

    public Write(Parameters parameters) {

        super(parameters);

        indexFormat = new Context.StringFormat(parameters.indexFormat);
        idFormat = parameters.idFormat == null ? null : new Context.StringFormat(parameters.idFormat);
        try {
            mapping = parameters.mapping == null ? null : Files.readString(Paths.get(parameters.mapping));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read mapping from " + parameters.mapping);
        }
    }

    @Override
    protected void setup(Setup setup) {
        setup.preferUpstreamAsync();
        setup.limitWorkers(1);
    }

    @Override
    protected void open() {
        super.open();

        BulkProcessor.Builder builder = BulkProcessor.builder(
                (request, bulkListener) -> restClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                this
        );

        builder.setFlushInterval(TimeValue.timeValueSeconds(10L));
        builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 3));

        bulkProcessor = builder.build();
    }

    @Override
    protected synchronized void transform(Context context, byte[] input) throws IOException {

        String index = indexFormat.format(context);

        if (mapping != null) {
            if (!indicesSeen.contains(index)) {
                if (!restClient.indices().exists(new GetIndexRequest(index), RequestOptions.DEFAULT)) {
                    CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
                    createIndexRequest.mapping(mapping, XContentType.JSON);
                    restClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                }
                indicesSeen.add(index);
            }
        }

        IndexRequest indexRequest = new IndexRequest(index);
        if (idFormat != null) indexRequest.id(idFormat.format(context));
        indexRequest.source(input, XContentType.JSON);
        bulkProcessor.add(indexRequest);
    }

    @Override
    protected void close() throws Exception {
        bulkProcessor.awaitClose(1, TimeUnit.HOURS);
        super.close();
    }

    @Override
    public void beforeBulk(long l, BulkRequest bulkRequest) {

    }

    @Override
    public void afterBulk(long l, BulkRequest bulkRequest, BulkResponse bulkResponse) {
        if (!bulkResponse.hasFailures()) {
            ack(bulkResponse.getItems().length);
        } else {
            for (BulkItemResponse itemResponse : bulkResponse) {
                if (!itemResponse.isFailed()) {
                    ack();
                } else {
                    // TODO
                }
            }
        }
    }

    @Override
    public void afterBulk(long l, BulkRequest bulkRequest, Throwable throwable) {

    }

    public static class Parameters extends ElasticsearchTransformer.Parameters {

        public String indexFormat;
        public String idFormat;
        public String mapping;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String indexFormat) {
            this.indexFormat = indexFormat;
        }
    }
}
