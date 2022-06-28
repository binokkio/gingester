package b.nana.technology.gingester.transformers.base.transformers.bigdecimal;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public final class Multiply implements Transformer<BigDecimal, BigDecimal> {

    private final BigDecimal multiplicand;
    private final String description;

    public Multiply(Parameters parameters) {
        multiplicand = parameters.multiplicand;
        description = "* " + multiplicand;
    }

    @Override
    public void transform(Context context, BigDecimal in, Receiver<BigDecimal> out) {
        out.accept(context.stash("description", description), in.multiply(multiplicand));
    }

    public static class Parameters {

        @JsonFormat(shape=JsonFormat.Shape.STRING)
        public BigDecimal multiplicand;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String multiplicand) {
            this.multiplicand = new BigDecimal(multiplicand);
        }
    }
}
