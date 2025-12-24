package com.nocker.portscanner.schedulers;

public interface PortScanScheduler {

    /**
     * Submits a runnable task to the scheduler.
     *
     * @param task a {@link Runnable}
     */
    void submit(Runnable task);

    /**
     * Graceful shutdown of any concurrent executor service utilized
     * by the scheduler.
     */
    void shutdownAndWait();
}
