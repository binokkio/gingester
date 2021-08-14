package b.nana.technology.gingester.core;

class TransformJob implements Worker.Job {

    private final Worker worker;
    final Gingester gingester;
    final Transformer<?, ?> transformer;
    final Object lock = new Object();
    volatile boolean starving;
//        long lastBatchReport = System.nanoTime();

    TransformJob(Worker worker, Gingester gingester, Transformer<?, ?> transformer) {
        this.worker = worker;
        this.gingester = gingester;
        this.transformer = transformer;
    }

    Worker getWorker() {
        return worker;
    }

    @Override
    public void run() {
        run(transformer);
    }

    private <I, O> void run(Transformer<I, O> transformer) {

        try {

            while (true) {

                Batch<? extends I> batch = transformer.queue.poll();
                if (batch == null) {
                    synchronized (lock) {
                        starving = true;
                        try {
                            while ((batch = transformer.queue.poll()) == null) {
                                gingester.signalStarving(this);
                                lock.wait();
                            }
                        } catch (InterruptedException e) {
                            break;
                        } finally {
                            starving = false;
                        }
                    }
                }

                if (transformer.maxBatchSize == 1) {
                    // no batch optimizations possible, not tracking batch duration
                    worker.transform(transformer, batch);
                } else {

                    long batchStarted = System.nanoTime();
                    worker.transform(transformer, batch);
                    long batchFinished = System.nanoTime();
                    double batchDuration = batchFinished - batchStarted;

                    if ((batchDuration < 2_000_000 && batch.getSize() != transformer.maxBatchSize) ||
                            (batchDuration > 4_000_000 && batch.getSize() != 1)) {

                        double abrupt = 3_000_000 / batchDuration * batch.getSize();
                        double dampened = (abrupt + batch.getSize() * 9) / 10;
                        transformer.batchSize = (int) Math.min(transformer.maxBatchSize, dampened);
                    }

//                        if (lastBatchReport + 1_000_000_000 < batchFinished) {
//                            lastBatchReport = batchFinished;
//                            System.err.printf(
//                                    "%s processed batch of %,d items in %,.3f seconds%n",
//                                    transformer.name,
//                                    batch.getSize(),
//                                    batchDuration / 1_000_000_000
//                            );
//                        }
                }

                Transformer.Statistics statistics = transformer.getStatistics().orElse(null);
                if (statistics != null) statistics.delt.addAndGet(batch.getSize());
            }

        } catch (Throwable t) {
            t.printStackTrace();  // TODO
            throw t;
        }
    }
}
