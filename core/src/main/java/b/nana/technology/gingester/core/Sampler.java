package b.nana.technology.gingester.core;

import java.util.Arrays;
import java.util.function.LongSupplier;

public class Sampler {

    private static final int SAMPLE_COUNT = 10;

    private final LongSupplier supplier;
    private final Sample[] samples = new Sample[SAMPLE_COUNT];
    private int pointer = 1;

    public Sampler(LongSupplier supplier) {
        this.supplier = supplier;
        Sample init = new Sample(supplier.getAsLong());
        Arrays.fill(samples, init);
    }

    public void sample() {
        samples[pointer++ % SAMPLE_COUNT] = new Sample(supplier.getAsLong());
    }

    public double getChangePerSecond() {
        Sample oldest = samples[pointer % SAMPLE_COUNT];
        Sample newest = samples[(pointer - 1) % SAMPLE_COUNT];
        double deltaValue = newest.value - oldest.value;
        double deltaNano = newest.nanos - oldest.nanos;
        return deltaValue / deltaNano * 1_000_000_000;
    }

    private static class Sample {

        private final long value;
        private final long nanos;

        private Sample(long value) {
            this.value = value;
            this.nanos = System.nanoTime();
        }
    }
}
