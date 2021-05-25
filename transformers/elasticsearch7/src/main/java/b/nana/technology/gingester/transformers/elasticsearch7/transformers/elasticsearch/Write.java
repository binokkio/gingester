package b.nana.technology.gingester.transformers.elasticsearch7.transformers.elasticsearch;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.elasticsearch7.common.ElasticsearchTransformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.util.concurrent.TimeUnit;

public class Write extends ElasticsearchTransformer<byte[], Void> implements BulkProcessor.Listener {

    private final Context.StringFormat indexFormat;
    private final Context.StringFormat idFormat;
    private BulkProcessor bulkProcessor;

    public Write(Parameters parameters) {

        super(parameters);

        indexFormat = new Context.StringFormat(parameters.indexFormat);
        idFormat = parameters.idFormat == null ? null : new Context.StringFormat(parameters.idFormat);
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
    protected synchronized void transform(Context context, byte[] input) {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(indexFormat.format(context));
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

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String indexFormat) {
            this.indexFormat = indexFormat;
        }
    }
}
