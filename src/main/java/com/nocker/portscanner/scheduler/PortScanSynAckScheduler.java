package com.nocker.portscanner.scheduler;

import org.apache.logging.log4j.core.util.UuidUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * {@code PortScanSynAckScheduler} schedules valid SYN or SYN ACK tasks.
 */
public class PortScanSynAckScheduler implements PortScanScheduler {
    private final ExecutorService executorService;
    private final List<Future<?>> submittedTasks = new CopyOnWriteArrayList<>();
    private int concurrency = 100; // adjustable
    private final UUID schedulerId = UuidUtil.getTimeBasedUuid();

     public PortScanSynAckScheduler() {
         this.executorService = Executors.newFixedThreadPool(concurrency);
     }

    public PortScanSynAckScheduler(int concurrency) {
         this.concurrency = concurrency;
         this.executorService = Executors.newFixedThreadPool(concurrency);
    }

    @Override
    public <T> void submit(Callable<T> task) {
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
        return results;
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
