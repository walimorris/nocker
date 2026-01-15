package com.nocker.portscanner.scheduler;

import com.nocker.portscanner.command.InvocationRequest;
import com.nocker.portscanner.report.PortScanReport;
import com.nocker.portscanner.report.PortScanResult;

import java.io.Serializable;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public interface PortScanScheduler extends Serializable {

    /**
     * Submits a port scanning task to the scheduler for asynchronous execution.
     *
     * @param task the task to be executed, represented as a {@code Callable}
     *             that produces a {@code List<PortScanResult>} upon completion
     */
    void submit(Callable<List<PortScanResult>> task);

    /**
     * Shuts down the scheduler, waits for the completion of all submitted tasks, gathers their results,
     * and compiles a comprehensive port scan report.
     *
     * @param taskCount the atomic counter representing the total number of tasks submitted to the scheduler.
     *                  This value is used to determine how many tasks the method needs to wait for
     *                  before shutting down the scheduler and collecting results.
     *
     * @return a {@code PortScanReport} instance that contains the results of all completed port scan tasks
     *         and a summary of the scan, including any aggregated data and statistical information.
     */
    PortScanReport shutdownAndCollect(AtomicInteger taskCount);

    /**
     * Retrieves the duration of time in milliseconds that elapsed between the start
     * of task execution and the shutdown of the scheduler. If either the start or
     * stop time is not initialized, an empty {@code OptionalLong} is returned.
     *
     * @return an {@code OptionalLong} containing the elapsed time in milliseconds
     *         if both start and stop times are available; otherwise, an empty {@code OptionalLong}.
     */
    public OptionalLong getDurationMillis();

    /**
     * Retrieves the duration of time in milliseconds that elapsed during
     * the execution of the tasks in the current batch. This value is determined
     * by the difference between the latest start time of the batch and the
     * stop time of the scheduler. If either the latest start time or
     * stop time is not initialized, an empty {@code OptionalLong} is returned.
     *
     * @return an {@code OptionalLong} containing the elapsed time in milliseconds
     *         for the current batch if both the latest start and stop times are
     *         available; otherwise, an empty {@code OptionalLong}.
     */
    public OptionalLong getDurationMillisBatch();

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

    /**
     * Retrieves the {@code InvocationCommand} associated with the scheduler.
     *
     * @return the {@code InvocationCommand} instance containing information
     *         about the command-line input, the target method to invoke,
     *         and its corresponding arguments
     */
    InvocationRequest getInvocationCommand();
}
