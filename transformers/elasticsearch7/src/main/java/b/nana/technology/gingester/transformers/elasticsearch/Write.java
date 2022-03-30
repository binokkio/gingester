package b.nana.technology.gingester.transformers.elasticsearch;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.reporting.Counter;
import b.nana.technology.gingester.core.reporting.SimpleCounter;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class Write extends ElasticsearchTransformer<byte[], Void> implements BulkProcessor.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Write.class);

    private final Template indexTemplate;
    private final Template idTemplate;
    private final String mapping;
    private final Set<String> indicesSeen = new HashSet<>();
    private BulkProcessor bulkProcessor;
    private final Counter acksCounter = new SimpleCounter();

    public Write(Parameters parameters) {

        super(parameters);

        indexTemplate = Context.newTemplate(parameters.index);
        idTemplate = parameters.id == null ? null : Context.newTemplate(parameters.id);
        try {
            mapping = parameters.mapping == null ? null : Files.readString(Paths.get(parameters.mapping));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read mapping from " + parameters.mapping);
        }
    }

    @Override
    public void setup(SetupControls controls) {
        controls.maxWorkers(1);
    }

    @Override
    public void open() {
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
    public void transform(Context context, byte[] in, Receiver<Void> out) throws Exception {

        String index = indexTemplate.render(context);

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
        if (idTemplate != null) indexRequest.id(idTemplate.render(context));
        indexRequest.source(in, XContentType.JSON);
        bulkProcessor.add(indexRequest);
    }

    @Override
    public void close() throws Exception {
        bulkProcessor.awaitClose(1, TimeUnit.HOURS);
        super.close();
    }

    @Override
    public void beforeBulk(long l, BulkRequest bulkRequest) {

    }

    @Override
    public void afterBulk(long l, BulkRequest bulkRequest, BulkResponse bulkResponse) {
        if (!bulkResponse.hasFailures()) {
            acksCounter.count(bulkResponse.getItems().length);
        } else {
            Set<String> failureMessages = new HashSet<>();
            for (BulkItemResponse itemResponse : bulkResponse) {
                if (!itemResponse.isFailed()) {
                    acksCounter.count();
                } else {
                    failureMessages.add(itemResponse.getFailureMessage());
                }
            }
            failureMessages.forEach(message -> LOGGER.warn("Bulk item failed: {}", message));
        }
    }

    @Override
    public void afterBulk(long l, BulkRequest bulkRequest, Throwable throwable) {
        LOGGER.warn("Bulk request failed", throwable);
    }

    public static class Parameters extends ElasticsearchTransformer.Parameters {

        public TemplateParameters index;
        public TemplateParameters id;
        public String mapping;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters index) {
            this.index = index;
        }
    }
}
