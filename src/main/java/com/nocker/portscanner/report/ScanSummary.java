package com.nocker.portscanner.report;

import com.nocker.portscanner.PortState;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nocker.portscanner.PortState.*;

/**
 * {@code ScanSummary} Represents a summary of a port scanning session.
 * This class tracks the count of ports in different states (open, closed,
 * filtered), the total number of ports scanned, and records the duration
 * of the scan. It also maintains a collection of open ports detected
 * during the scan.
 *
 * @author Wali Morris
 */
public class ScanSummary {
    private final AtomicInteger openPortsCount = new AtomicInteger();
    private final AtomicInteger filteredPortsCount = new AtomicInteger();
    private final AtomicInteger closedPortsCount = new AtomicInteger();
    private final AtomicInteger totalPortsScanned = new AtomicInteger();
    private final Queue<Integer> openPorts = new ConcurrentLinkedQueue<>();
    private final long startTime;
    private long stopTime;

    public ScanSummary(long startNanos) {
        this.startTime = startNanos;
    }

    /**
     * Updates the scan summary based on the given port scan result.
     * This method increments the appropriate counters for open,
     * filtered, or closed ports and adds the port to the open ports
     * queue if its state is {@code OPEN}.
     *
     * @param result the {@code PortScanResult} containing information
     *              about the scanned port, including its state
     */
    public void update(PortScanResult result) {
        PortState state = result.getState();
        totalPortsScanned.incrementAndGet();
        if (state.equals(OPEN)) {
            openPortsCount.incrementAndGet();
            openPorts.add(result.getPort());
        } else if (state.equals(FILTERED)) {
            filteredPortsCount.incrementAndGet();
        } else {
            if (state.equals(CLOSED)) {
                closedPortsCount.incrementAndGet();
            }
        }
    }

    /**
     * Calculates the duration of the scan in milliseconds.
     * The duration is determined as the difference between
     * the stop time and the start time of the scan,
     * converted to milliseconds.
     *
     * @return the total duration of the scan in milliseconds
     */
    public long durationMillis() {
        return TimeUnit.MILLISECONDS.toMillis(stopTime - startTime);
    }

    /**
     * Stops the timer for the scan by recording the current
     * time in nanoseconds. This method sets the `stopTime`
     * field to the value returned by {@link System#nanoTime()},
     * marking the end of the scanning process.
     */
    public void stop() {
        this.stopTime = System.nanoTime();
    }

    /**
     * Retrieves a queue containing the ports that have been
     * identified as open during the port scanning process.
     * The queue stores the port numbers in the order in
     * which they were detected as open.
     *
     * @return a queue of integers representing the list
     * of open ports
     */
    public Queue<Integer> getOpenPorts() {
        return openPorts;
    }

    /**
     * Retrieves the count of ports identified as open during
     * the scan.
     *
     * @return the number of open ports detected during the
     * scanning process
     */
    public int getOpenPortsCount() {
        return this.openPortsCount.get();
    }

    /**
     * Retrieves the count of ports that were identified as
     * filtered during the scanning process. Filtered ports
     * are those for which the scan result was inconclusive,
     * likely due to interference from firewalls or network
     * configurations.
     *
     * @return the number of filtered ports detected during
     * the scan
     */
    public int getFilteredPortsCount() {
        return this.filteredPortsCount.get();
    }

    /**
     * Retrieves the count of ports that were identified as
     * closed during the scanning process. Closed ports are
     * those that were determined to be non-responsive or
     * explicitly rejecting connection attempts.
     *
     * @return the number of closed ports detected during the
     * scanning process
     */
    public int getClosedPortsCount() {
        return this.closedPortsCount.get();
    }

    /**
     * Retrieves the total number of ports that have been
     * scanned. This includes all ports regardless of their
     * final state (open, closed, filtered).
     *
     * @return the total number of ports scanned during
     * the process
     */
    public int getTotalPortsScanned() {
        return this.totalPortsScanned.get();
    }
}
