package b.nana.technology.gingester.transformers.jetty.http.mock;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MockServer implements Transformer<Object, InputStream> {

    private final List<RequestResponse> playbook = new ArrayList<>();

    public MockResponse addRequest(MockRequest.Builder request) {
        MockResponse responseDummy = new MockResponse();
        playbook.add(new RequestResponse(request.build(), responseDummy));
        return responseDummy;
    }

    @Override
    public void transform(Context context, Object in, Receiver<InputStream> out) throws IOException {

        for (RequestResponse requestResponse : playbook) {

            Map<String, Object> stash = Map.of(
                    "http", Map.of(
                            "request", requestResponse.request.getStash(),
                            "response", requestResponse.response
                    )
            );

            InputStream requestBody = requestResponse.request.getRequestBody();
            out.accept(context.stash(stash), requestBody);
            requestBody.close();
        }
    }

    private static class RequestResponse {

        final MockRequest request;
        final MockResponse response;

        RequestResponse(MockRequest request, MockResponse response) {
            this.request = request;
            this.response = response;
        }
    }
}
