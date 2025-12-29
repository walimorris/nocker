package com.nocker.portscanner.scheduler;

import com.nocker.portscanner.PortScanResult;
import com.nocker.portscanner.PortScanner;
import org.apache.logging.log4j.core.util.UuidUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code PortScanSynAckScheduler} schedules valid SYN or SYN ACK tasks.
 */
// review: duration times - because of update
public class PortScanSynAckScheduler implements PortScanScheduler {
    private final ExecutorService executorService;
    private final CompletionService<List<PortScanResult>> completionService;
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
         this.completionService = new ExecutorCompletionService<>(executorService);
    }

    @Override
    public void submit(Callable<List<PortScanResult>> task) {
        long now = System.nanoTime();
        startNanos.compareAndSet(0, now);
        completionService.submit(task);
    }

    @Override
    public List<PortScanResult> shutdownAndCollect(AtomicInteger taskCount) {
        List<PortScanResult> results = new ArrayList<>();
        try {
            int i = 0;
            while (i < taskCount.get()) {
                Future<List<PortScanResult>> future = completionService.take();
                List<PortScanResult> tasksResults = future.get();
                if (tasksResults != null) {
                    results.addAll(tasksResults);
                }
                i++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            // log something useful
        } finally {
            executorService.shutdown();
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
