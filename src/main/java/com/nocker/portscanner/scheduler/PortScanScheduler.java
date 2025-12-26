package com.nocker.portscanner.scheduler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public interface PortScanScheduler {

    /**
     * Submits a task for execution to the scheduler's executor service.
     * The task is represented as a {@link Callable} that can return a result upon completion.
     * Submitted tasks are tracked by the scheduler for later processing or result retrieval.
     *
     * @param <T> the type of the result produced by the task
     * @param task the task to submit for execution, implemented as a {@link Callable}
     */
    <T> void submit(Callable<T> task);

    /**
     * Shuts down the scheduler's executor service and collects the results of all submitted tasks
     * that match the specified result type. The executor service will not accept new tasks after
     * the shutdown is initiated.
     *
     * @param <T> the type of results to collect
     * @param resultType the class type of results to be collected
     * @return a list of results of type {@code T} from the completed tasks
     */
    <T> List<T> shutdownAndCollect(Class<T> resultType);

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
