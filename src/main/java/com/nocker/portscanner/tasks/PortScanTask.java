package com.nocker.portscanner.tasks;

import java.io.Serializable;

public interface PortScanTask extends Serializable {

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

    /**
     * Retrieves the range of destination ports associated with the port scan task.
     *
     * @return the range of destination ports as a {@link PortRange} object; it defines
     *         the lower and upper bounds of the ports being scanned.
     */
    PortRange getDestinationPortRange();
}
