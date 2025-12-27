package com.nocker.portscanner.scheduler;

import org.apache.logging.log4j.core.util.UuidUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code PortScanSynAckScheduler} schedules valid SYN or SYN ACK tasks.
 */
public class PortScanSynAckScheduler implements PortScanScheduler {
    private final ExecutorService executorService;
    private final List<Future<?>> submittedTasks = new CopyOnWriteArrayList<>();
    private final int concurrency; // adjustable
    private final UUID schedulerId = UuidUtil.getTimeBasedUuid();

    private final AtomicLong startNanos = new AtomicLong(0);
    private volatile long stopNanos;

    private static final int DEFAULT_CONCURRENCY = 100;

     public PortScanSynAckScheduler() {
         this(DEFAULT_CONCURRENCY);
     }

    public PortScanSynAckScheduler(int concurrency) {
         this.concurrency = concurrency;
         this.executorService = Executors.newFixedThreadPool(concurrency);
    }

    @Override
    public <T> void submit(Callable<T> task) {
         startNanos.compareAndSet(0, System.nanoTime());
         Future<T> future = executorService.submit(task);
         submittedTasks.add(future);
    }

    @Override
    public <T> List<T> shutdownAndCollect(Class<T> resultType) {
         executorService.shutdown();
         try {
             executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
         } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
         }
         List<T> results = new ArrayList<>();
         for (Future<?> future : submittedTasks) {
            try {
                Object result = future.get();
                if (resultType.isInstance(result)) {
                    results.add(resultType.cast(result));
                }
            } catch (ExecutionException e) {
                // do something
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
         stopNanos = System.nanoTime();
        return results;
    }

    @Override
    public OptionalLong getDurationMillis() {
         long start = startNanos.get();
         long stop = stopNanos;
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
