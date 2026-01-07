package com.nocker.portscanner.report;

import com.nocker.OperatingSystemUtils;
import com.nocker.portscanner.PortState;
import com.nocker.portscanner.command.InvocationCommand;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final AtomicInteger openPortsCount;
    private final AtomicInteger filteredPortsCount;
    private final AtomicInteger closedPortsCount;
    private final AtomicInteger totalPortsScanned;
    private final ConcurrentHashMap<String, Set<Integer>> openHostPorts;
    private final UUID schedulerId;
    private final InvocationCommand invocationCommand;
    private final long startTime;
    private long stopTime;
    private final long durationMillis;

    private static final String NEW_LINE = "\n";

    public ScanSummary(long startNanos, UUID schedulerId, InvocationCommand command) {
        this(startNanos,
                schedulerId,
                command,
                0L,
                0L,
                new AtomicInteger(),
                new AtomicInteger(),
                new AtomicInteger(),
                new AtomicInteger(),
                new ConcurrentHashMap<>()
        );
    }

    public ScanSummary(long startNanos,
                       UUID schedulerId,
                       InvocationCommand command,
                       long stopTime,
                       long durationMillis,
                       AtomicInteger openPortsCount,
                       AtomicInteger filteredPortsCount,
                       AtomicInteger closedPortsCount,
                       AtomicInteger totalPortsScanned,
                       Map<String, Set<Integer>> openHostPorts) {
        this.startTime = startNanos;
        this.schedulerId = schedulerId;
        this.invocationCommand = command;
        this.stopTime = stopTime;
        this.durationMillis = durationMillis;
        this.openPortsCount = openPortsCount;
        this.filteredPortsCount = filteredPortsCount;
        this.closedPortsCount = closedPortsCount;
        this.totalPortsScanned = totalPortsScanned;
        this.openHostPorts = new ConcurrentHashMap<>(openHostPorts);
    }

    /**
     * Updates the scan summary with the results of a port scan.
     * This method processes the provided {@link PortScanResult}
     * to update statistics about the total number of ports
     * scanned, as well as the number of open, closed, and
     * filtered ports. It also tracks which specific ports are
     * open for each host.
     *
     * @param result the result of a single port scan to be
     *               incorporated into the scan summary. This
     *               includes details about the scanned port's
     *               state (e.g., open, closed, filtered), the
     *               host address, and the port number.
     */
    public void update(PortScanResult result) {
        PortState state = result.getState();
        totalPortsScanned.incrementAndGet();
        String currentHost = result.getHostAddress().getHostAddress();
        int currentPort = result.getPort();
        if (state.equals(OPEN)) {
            openHostPorts.computeIfAbsent(currentHost, host -> ConcurrentHashMap.newKeySet())
                    .add(currentPort);
            openPortsCount.incrementAndGet();
        } else if (state.equals(FILTERED)) {
            filteredPortsCount.incrementAndGet();
        } else {
            if (state.equals(CLOSED)) {
                closedPortsCount.incrementAndGet();
            }
        }
    }

    /**
     * Calculates and returns the duration of the scan
     * in milliseconds. If the duration has been
     * precomputed and stored, it is returned directly.
     * Otherwise, the duration is calculated as the
     * difference between the stop time and the start
     * time, converted from nanoseconds to milliseconds.
     *
     * @return the duration of the scan in milliseconds
     */
    public long durationMillis() {
        if (this.durationMillis == 0L) {
            return TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        }
        return durationMillis;
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

    /**
     * Retrieves a map of hosts and their corresponding sets
     * of open ports that were detected during the scanning
     * process. Each entry in the map associates a host address
     * (as a string) to a set of integers representing the
     * ports that were identified as open.
     *
     * @return a map where the keys are host addresses as strings,
     *         and the values are sets of integers representing
     *         open ports
     */
    public Map<String, Set<Integer>> getOpenHostPorts() {
        return this.openHostPorts;
    }

    /**
     * Retrieves the identifier of the scheduler associated with
     * this scan.
     *
     * @return the UUID that represents the scheduler ID
     */
    public UUID getSchedulerId() {
        return this.schedulerId;
    }

    /**
     * Retrieves the {@link InvocationCommand} associated with this
     * scan summary. The {@link InvocationCommand} represents the
     * command-line input, method, and arguments that were used to
     * initiate the scan process.
     *
     * @return the {@link InvocationCommand} containing details about
     * the scan invocation
     */
    public InvocationCommand getInvocationCommand() {
        return this.invocationCommand;
    }

    @Override
    public String toString() {
        return "";
    }

    /**
     * Generates a detailed text summary of the scan process,
     * including information about the user who initiated the
     * scan, the command used, the duration of the scan, the
     * scheduler ID, and port statistics. Additionally, it
     * provides a breakdown of open ports organized by host.
     *
     * @return a string containing the detailed scan summary
     */
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Summary for: %s", OperatingSystemUtils.currentUser()))
                .append(NEW_LINE)
                .append(String.format("Invocation Command: %s", invocationCommand.getCommandLineInput().getCommand()))
                .append(NEW_LINE)
                .append(String.format("Duration: %d", durationMillis()))
                .append(NEW_LINE)
                .append(String.format("Scheduler: %s", schedulerId))
                .append(NEW_LINE)
                .append(String.format("Total Ports Scanned: %d", totalPortsScanned.get()))
                .append(NEW_LINE)
                .append(String.format("Open Ports Count: %d", openPortsCount.get()))
                .append(NEW_LINE)
                .append(String.format("Closed Ports Count: %d", closedPortsCount.get()))
                .append(NEW_LINE)
                .append(String.format("Filtered Ports Count: %d", filteredPortsCount.get()))
                .append(NEW_LINE)
                .append("Breakdown of Open Ports by host: ").append(NEW_LINE);

        for (Map.Entry<String, Set<Integer>> entry : openHostPorts.entrySet()) {
            sb.append(String.format("Host: %s    [%s]", entry.getKey(), entry.getValue())).append(NEW_LINE);
        }
        return sb.toString();
    }

    /**
     * Converts the current scan summary into a {@link SummaryNode} object.
     * The created {@link SummaryNode} contains details such as the user who
     * initiated the scan, the command used, the duration of the scan, the
     * scheduler ID, the total number of ports scanned, and the counts of
     * open, closed, and filtered ports. It also includes the mapping of hosts
     * to their open ports.
     *
     * @return a {@link SummaryNode} object encapsulating the details of the
     * scan summary
     */
    public SummaryNode toSummaryNode() {
        return new SummaryNode(
                OperatingSystemUtils.currentUser(),
                invocationCommand.getCommandLineInput().getCommand(),
                durationMillis(),
                schedulerId,
                totalPortsScanned.get(),
                openPortsCount.get(),
                closedPortsCount.get(),
                filteredPortsCount.get(),
                openHostPorts
        );
    }
}
