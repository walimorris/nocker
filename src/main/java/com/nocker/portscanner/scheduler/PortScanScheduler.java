package com.nocker.portscanner.scheduler;

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

    /**
     * Retrieves the unique identifier of the scheduler.
     *
     * @return a string representing the unique ID of the scheduler
     */
    String getSchedulerIdText();
}
