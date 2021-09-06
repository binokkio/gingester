package b.nana.technology.gingester.core.reporting;

import java.util.Arrays;
import java.util.function.LongSupplier;

public final class Sampler {

    private static final int SAMPLE_COUNT = 6;

    private final LongSupplier supplier;
    private Sample epoch;
    private final Sample[] samples = new Sample[SAMPLE_COUNT];
    private int pointer = 1;

    public Sampler(LongSupplier supplier) {
        this.supplier = supplier;
        epoch();
    }

    public void epoch() {
        epoch = new Sample(supplier.getAsLong());
        Arrays.fill(samples, epoch);
    }

    public void sample() {
        samples[pointer++ % SAMPLE_COUNT] = new Sample(supplier.getAsLong());
    }

    public long getValue() {
        return samples[(pointer - 1) % SAMPLE_COUNT].value;
    }

    public double getCurrentChangePerSecond() {
        Sample oldest = samples[pointer % SAMPLE_COUNT];
        Sample newest = samples[(pointer - 1) % SAMPLE_COUNT];
        return getChangePerSecond(oldest, newest);
    }

    public long getCurrentNanos() {
        Sample oldest = samples[pointer % SAMPLE_COUNT];
        Sample newest = samples[(pointer - 1) % SAMPLE_COUNT];
        return newest.nanos - oldest.nanos;
    }

    public double getEpochChangePerSecond() {
        Sample oldest = epoch;
        Sample newest = samples[(pointer - 1) % SAMPLE_COUNT];
        return getChangePerSecond(oldest, newest);
    }

    public long getEpochNanos() {
        Sample newest = samples[(pointer - 1) % SAMPLE_COUNT];
        return newest.nanos - epoch.nanos;
    }

    private static double getChangePerSecond(Sample oldest, Sample newest) {
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
