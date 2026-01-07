package com.nocker.portscanner.scheduler;

import com.nocker.portscanner.PortScanner;
import com.nocker.portscanner.command.InvocationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code PortScanSynAckSchedulerFactory} is responsible for creating instances of
 * {@code PortScanSynAckScheduler}. Provides configuration for controlling port
 * scanning tasks using the SYN-ACK strategy, enabling concurrency and invocation
 * commands.
 *<p>
 * Supports configuration for concurrency levels as well as the specific invocation
 * commands required for scheduling and managing port scans.
 *
 * @author Wali morris
 */
public class PortScanSynAckSchedulerFactory implements PortScanSchedulerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortScanSynAckSchedulerFactory.class);

    private final InvocationCommand invocationCommand;
    private final int concurrency;

    public PortScanSynAckSchedulerFactory(InvocationCommand invocationCommand, int concurrency) {
        this.invocationCommand = invocationCommand;
        this.concurrency = concurrency;
    }

    public PortScanSynAckSchedulerFactory(InvocationCommand invocationCommand) {
        this(invocationCommand, PortScanner.DEFAULT_CONCURRENCY);
    }

    @Override
    public PortScanScheduler create() {
        try {
            return new PortScanSynAckScheduler(concurrency, invocationCommand);
        } catch (Exception e) {
            LOGGER.error("Error creating PortScanScheduler: {}", e.getMessage());
            throw e;
        }
    }
}
