package com.nocker.portscanner.scheduler;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

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

    /**
     * Retrieves the unique identifier of the scheduler.
     *
     * @return a {@code UUID} representing the scheduler's unique identifier
     */
    UUID getSchedulerId();

    /**
     * Retrieves the configured concurrency level for the scheduler.
     *
     * @return the concurrency level as an integer, indicating the number of threads
     *         or parallel tasks the scheduler supports.
     */
    int getConcurrency();

    /**
     * Retrieves the {@link ExecutorService} instance used by the scheduler.
     *
     * @return the ExecutorService responsible for managing task execution
     */
    ExecutorService getExecutorService();
}
