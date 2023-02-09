package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.transformers.jetty.http.mock.MockRequest;
import b.nana.technology.gingester.transformers.jetty.http.mock.MockResponse;
import b.nana.technology.gingester.transformers.jetty.http.mock.MockServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HttpTest {

    @Test
    void testHttpServerFlowCanBeTested() {

        MockServer mockServer = new MockServer();
        MockResponse response = mockServer.addRequest(new MockRequest
                .Builder("POST /")
                .setRequestBody("Hello, World!"));

        new FlowBuilder().cli("" +
                "-sft HttpServer " +
                "-t InputStreamToString " +
                "-t Repeat 3 " +
                "-t StringDef '${http.request.object.remoteAddress}: ${__in__}' " +
                "-stt InputStreamJoin " +
                "-t HttpRespond")
                .replace("HttpServer", mockServer)
                .run();

        assertEquals(
                "127.0.0.1: Hello, World!\n127.0.0.1: Hello, World!\n127.0.0.1: Hello, World!",
                response.getOutputStream().toString()
        );
    }
}
