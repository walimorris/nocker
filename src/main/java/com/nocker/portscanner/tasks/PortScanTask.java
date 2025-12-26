package com.nocker.portscanner.tasks;

public interface PortScanTask {

    /**
     * Retrieves the unique identifier of the task.
     *
     * @return a string representation of the task's unique identifier
     */
    String getTaskIdText();

    /**
     * Retrieves the unique identifier of the scheduler associated with the task.
     *
     * @return a string representation of the scheduler's unique identifier
     */
    String getSchedulerIdText();
}
