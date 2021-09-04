package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.controller.Controller;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;

public class TransformerConfiguration {

    public String transformer;
    public JsonNode parameters;

    @JsonUnwrapped
    public Controller.Parameters controllerParameters = new Controller.Parameters();

    @JsonCreator
    public TransformerConfiguration() {}

    @JsonCreator
    public TransformerConfiguration(String transformer) {
        this.transformer = transformer;
    }
}
