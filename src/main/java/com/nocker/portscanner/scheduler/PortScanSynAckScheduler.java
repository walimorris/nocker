package com.nocker.portscanner.scheduler;

import com.nocker.portscanner.command.InvocationCommand;
import com.nocker.portscanner.report.PortScanReport;
import com.nocker.portscanner.report.PortScanResult;
import com.nocker.portscanner.PortScanner;
import com.nocker.portscanner.report.ScanSummary;
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
    private final transient ExecutorService executorService;
    private final transient CompletionService<List<PortScanResult>> completionService;
    private final int concurrency; // adjustable
    private final UUID schedulerId = UuidUtil.getTimeBasedUuid();
    private final InvocationCommand invocationCommand;

    private final transient AtomicLong startNanos = new AtomicLong(0);
    private final transient AtomicLong latestStartNanos = new AtomicLong(0);
    private final transient AtomicLong stopNanos = new AtomicLong(0);

    private static final long serialVersionUID = 1L;

    public PortScanSynAckScheduler(InvocationCommand invocationCommand) {
        this(PortScanner.DEFAULT_CONCURRENCY, invocationCommand);
    }

    public PortScanSynAckScheduler(int concurrency, InvocationCommand invocationCommand) {
         this.concurrency = concurrency;
         this.executorService = Executors.newFixedThreadPool(concurrency);
         this.completionService = new ExecutorCompletionService<>(executorService);
         this.invocationCommand = invocationCommand;
    }

    @Override
    public void submit(Callable<List<PortScanResult>> task) {
        long now = System.nanoTime();
        startNanos.compareAndSet(0, now);
        completionService.submit(task);
    }

    @Override
    public PortScanReport shutdownAndCollect(AtomicInteger taskCount) {
        List<PortScanResult> results = new ArrayList<>();
        ScanSummary scanSummary = new ScanSummary(startNanos.get(), schedulerId, invocationCommand);
        try {
            for (int i = 0; i < taskCount.get(); i++) {
                Future<List<PortScanResult>> future = completionService.
                        poll(100, TimeUnit.MILLISECONDS);
                if (future != null) {
                    List<PortScanResult> tasksResults = future.get();
                    if (tasksResults != null) {
                        for (PortScanResult result : tasksResults) {
                            scanSummary.update(result);
                            results.add(result);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            // log something useful
        } finally {
            executorService.shutdownNow();
        }
        stopNanos.set(System.nanoTime());
        scanSummary.stop();
        return new PortScanReport(this, results, scanSummary);
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
    public InvocationCommand getInvocationCommand() {
        return invocationCommand;
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
