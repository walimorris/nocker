package com.nocker.portscanner.scheduler;

import com.nocker.portscanner.BatchState;
import com.nocker.portscanner.PortScanner;
import org.apache.logging.log4j.core.util.UuidUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code PortScanSynAckScheduler} schedules valid SYN or SYN ACK tasks.
 */
public class PortScanSynAckScheduler implements PortScanScheduler {
    private final ExecutorService executorService;
    private final AtomicLong currentBatch = new AtomicLong(0);
    private final Map<Long, BatchState> batches = new ConcurrentHashMap<>();
    private final int concurrency; // adjustable
    private final UUID schedulerId = UuidUtil.getTimeBasedUuid();

    private final AtomicLong startNanos = new AtomicLong(0);
    private final AtomicLong latestStartNanos = new AtomicLong(0);
    private final AtomicLong stopNanos = new AtomicLong(0);

     public PortScanSynAckScheduler() {
         this(PortScanner.DEFAULT_CONCURRENCY);
     }

    public PortScanSynAckScheduler(int concurrency) {
         this.concurrency = concurrency;
         this.executorService = Executors.newFixedThreadPool(concurrency);
    }

    @Override
    public <T> void submit(Callable<T> task) {
        long now = System.nanoTime();
        startNanos.compareAndSet(0, now);
        latestStartNanos.set(now); // batch duration monitor
         long batch = currentBatch.get();
         BatchState state = batches.computeIfAbsent(batch, b -> new BatchState());
         Future<T> future = executorService.submit(() -> {
             try {
                 return task.call();
             } finally {
                 state.onComplete();
             }
         });
         state.onSubmit(future);
    }

    @Override
    public <T> List<T> shutdownAndCollect(Class<T> resultType) {
        List<T> results = collect(resultType);
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        stopNanos.set(System.nanoTime());
        return results;
    }

    @Override
    public <T> List<T> collect(Class<T> resultType) {
        List<T> results = new ArrayList<>();
        long batch = currentBatch.get();
        BatchState state = batches.computeIfAbsent(batch, b -> new BatchState());
        state.seal();
        try {
            state.done.await();
            for (Future<?> future : state.futures) {
                Object result = future.get();
                if (resultType.isInstance(result)) {
                    results.add(resultType.cast(result));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            // do something
        } finally {
            currentBatch.incrementAndGet();
        }
        stopNanos.set(System.nanoTime());
        return results;
    }

    @Override
    public OptionalLong getDurationMillis() {
         long start = startNanos.get();
         long stop = stopNanos.get();
         return (start != 0 && stop != 0)
                 ? OptionalLong.of(TimeUnit.NANOSECONDS.toMillis(stop - start))
                 : OptionalLong.empty();
    }

    @Override
    public OptionalLong getDurationMillisBatch() {
        long start = latestStartNanos.get();
        long stop = stopNanos.get();
        return (start != 0 && stop != 0)
                ? OptionalLong.of(TimeUnit.NANOSECONDS.toMillis(stop - start))
                : OptionalLong.empty();
    }

    @Override
    public UUID getSchedulerId() {
         return schedulerId;
    }

    @Override
    public String getSchedulerIdText() {
        return schedulerId.toString();
    }

    @Override
    public int getConcurrency() {
         return this.concurrency;
    }

    @Override
    public ExecutorService getExecutorService() {
         return this.executorService;
    }

    @Override
    public String toString() {
        return "PortScanSynAckScheduler{" +
                "schedulerId=" + schedulerId +
                ", concurrency=" + concurrency +
                ", executorServiceStatus=" + (executorService.isShutdown() ? "SHUTDOWN" : "ACTIVE") +
                '}';
    }
}
