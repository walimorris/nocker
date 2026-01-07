package com.nocker.portscanner.scheduler;

public interface PortScanSchedulerFactory {

    /**
     * Creates a new instance of {@code PortScanScheduler}.
     *
     * @return a newly created instance of {@code PortScanScheduler}
     * capable of managing and executing port scanning tasks.
     */
    PortScanScheduler create();
}
