package com.nocker.portscanner.schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * {@code PortScanSynAckScheduler} schedules valid SYN or SYN ACK tasks.
 */
public class PortScanSynAckScheduler implements PortScanScheduler {
    private final ExecutorService executorService;
    private int concurrency = 100; // adjustable

     public PortScanSynAckScheduler() {
         this.executorService = Executors.newFixedThreadPool(concurrency);
     }

    public PortScanSynAckScheduler(int concurrency) {
         this.concurrency = concurrency;
         this.executorService = Executors.newFixedThreadPool(concurrency);
    }

    @Override
    public void submit(Runnable task) {
         executorService.submit(task);
    }

    @Override
    public void shutdownAndWait() {
         executorService.shutdown();
         try {
             executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
         } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
         }
    }

    public int getConcurrency() {
         return this.concurrency;
    }
}
