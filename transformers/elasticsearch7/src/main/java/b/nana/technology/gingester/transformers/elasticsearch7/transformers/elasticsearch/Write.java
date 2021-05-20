package b.nana.technology.gingester.transformers.elasticsearch7.transformers.elasticsearch;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.elasticsearch7.common.ElasticsearchTransformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.util.concurrent.TimeUnit;

public class Write extends ElasticsearchTransformer<byte[], Void> implements BulkProcessor.Listener {

    private final Context.StringFormat indexFormat;
    private final Context.StringFormat idFormat;
    private final BulkProcessor bulkProcessor;

    public Write(Parameters parameters) {

        super(parameters);

        indexFormat = new Context.StringFormat(parameters.indexFormat);
        idFormat = parameters.idFormat == null ? null : new Context.StringFormat(parameters.idFormat);

        BulkProcessor.Builder builder = BulkProcessor.builder(
                (request, bulkListener) -> restClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                this
        );

        builder.setFlushInterval(TimeValue.timeValueSeconds(10L));
        builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 3));

        bulkProcessor = builder.build();

        // TODO bulkProcess.awaitClose() on shutdown, maybe call it flush
    }

    @Override
    protected void setup(Setup setup) {
        setup.limitMaxWorkers(1);
    }

    @Override
    protected void transform(Context context, byte[] input) {
        synchronized (bulkProcessor) {
            IndexRequest indexRequest = new IndexRequest();
            indexRequest.index(indexFormat.format(context));
            if (idFormat != null) indexRequest.id(idFormat.format(context));
            indexRequest.source(input, XContentType.JSON);
            bulkProcessor.add(indexRequest);
        }
    }

    @Override
    protected void close() throws Exception {
        synchronized (bulkProcessor) {
            bulkProcessor.close();
            bulkProcessor.awaitClose(1, TimeUnit.HOURS);
            restClient.close();
        }
    }

    @Override
    public void beforeBulk(long l, BulkRequest bulkRequest) {

    }

    @Override
    public void afterBulk(long l, BulkRequest bulkRequest, BulkResponse bulkResponse) {

    }

    @Override
    public void afterBulk(long l, BulkRequest bulkRequest, Throwable throwable) {

    }

    public static class Parameters extends ElasticsearchTransformer.Parameters {

        public String indexFormat;
        public String idFormat;

        @JsonCreator
        public Parameters() {

        }

        @JsonCreator
        public Parameters(String indexFormat) {
            this.indexFormat = indexFormat;
        }
    }
}
