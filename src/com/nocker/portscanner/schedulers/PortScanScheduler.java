package com.nocker.portscanner.schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// we can adjust concurrency later based on process
public class PortScanScheduler {
    private final ExecutorService executorService;
    private int concurrency = 100;

     public PortScanScheduler() {
         this.executorService = Executors.newFixedThreadPool(concurrency);
     }

    public PortScanScheduler(int concurrency) {
         this.concurrency = concurrency;
         this.executorService = Executors.newFixedThreadPool(concurrency);
    }

    public void submit(Runnable task) {
         executorService.submit(task);
    }

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
