package com.nocker.portscanner;

import com.nocker.cli.PortScannerContext;
import com.nocker.cli.formatter.OutputFormatter;
import com.nocker.portscanner.annotation.arguments.Host;
import com.nocker.portscanner.annotation.arguments.Hosts;
import com.nocker.portscanner.annotation.arguments.Port;
import com.nocker.portscanner.command.InvocationCommand;
import com.nocker.portscanner.annotation.arguments.Ports;
import com.nocker.portscanner.annotation.commands.CIDRScan;
import com.nocker.portscanner.annotation.commands.Scan;
import com.nocker.portscanner.model.HostIdentity;
import com.nocker.portscanner.model.HostModel;
import com.nocker.portscanner.report.PortScanReport;
import com.nocker.portscanner.report.PortScanResult;
import com.nocker.portscanner.report.ScanSummary;
import com.nocker.portscanner.scheduler.PortScanScheduler;
import com.nocker.portscanner.scheduler.PortScanSchedulerFactory;
import com.nocker.portscanner.tasks.PortRange;
import com.nocker.portscanner.tasks.PortScanSynAckTask;
import com.nocker.portscanner.tasks.PortScanSynTask;
import com.nocker.portscanner.wildcard.CIDRWildcard;
import com.nocker.portscanner.wildcard.PortWildcard;
import com.nocker.writer.NockerFileWriter;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.nocker.portscanner.PortState.*;
import static com.nocker.portscanner.SourcePortAllocator.*;

// TODO: normalize arg inputs (ex: extra space in command will throw)
// TODO: formalize triggerResponse() with Reports instead of "loose-lists" etc
public class PortScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortScanner.class);

    private final int timeout;
    private final int concurrency;
    private final InvocationCommand invocationCommand;
    private final NockerFileWriter fileWriter;
    private final OutputFormatter outputFormatter;
    private final PortScanSchedulerFactory schedulerFactory;
    private final boolean sneak;
    private final boolean robust;

    /**
     * Minimum valid port number that can be scanned.
     */
    public static final int MIN_PORT = 1;

    /**
     * Maximum valid port number that can be scanned.
     */
    public static final int MAX_PORT = 65535;

    /**
     * Minimum number of ports in a single scan request before multi-threading.
     */
    public static final int MIN_PORTS_CONCURRENCY_USAGE = 1000;

    /**
     * Default timeout for a single port scan attempt, in milliseconds.
     */
    public static final int DEFAULT_TIMEOUT = 100;
    /**
     * Lower limit of acceptable scan timeout, in milliseconds.
     */
    public static final int TIME_OUT_LOW_LIMIT = 50;

    /**
     * Upper limit of acceptable scan timeout, in milliseconds.
     */
    public static final int TIME_OUT_HIGH_LIMIT = 200;

    /**
     * Target number of concurrent port scan threads by default.
     */
    public static final int DEFAULT_CONCURRENCY = 100;

    /**
     * The maximum number of concurrent {@code PortScanScheduler} instances.
     */
    public static final int MAX_SCHEDULERS = 3;

    /**
     * Allocator for ephemeral source ports used during scans,
     * constrained between MIN_EPHEMERAL_PORT and MAX_EPHEMERAL_PORT.
     * @see SourcePortAllocator#MIN_EPHEMERAL_PORT
     * @see SourcePortAllocator#MAX_EPHEMERAL_PORT
     */
    public static final SourcePortAllocator sourcePortAllocator = new SourcePortAllocator(MIN_EPHEMERAL_PORT, MAX_EPHEMERAL_PORT);

    /**
     * Average duration of scanning a single port on a local host, in milliseconds.
     */
    public static final float AVG_LOCAL_SCAN_MS = 0.05F;

    /**
     * Target completion time for a single scanning task on a local host, in milliseconds.
     */
    public static final int TARGET_TASK_COMPLETION_LOCAL_MS = 100;

    /**
     * Average duration of scanning a single port on a remote host, in milliseconds.
     */
    public static final int AVG_REMOTE_SCAN_MS = 60;

    /**
     * Target completion time for a single scanning task on a remote host, in milliseconds.
     */
    public static final int TARGET_TASK_COMPLETION_REMOTE_MS = 500;

    /**
     * Minimum number of ports in a chunk when scanning a local host.
     */
    public static final int CHUNK_PORTS_LOCAL_MIN = 500;

    /**
     * Maximum number of ports in a chunk when scanning a local host.
     */
    public static final int CHUNK_PORTS_LOCAL_MAX = 3000;

    /**
     * Minimum number of ports in a chunk when scanning a remote host.
     */
    public static final int CHUNK_PORTS_REMOTE_MIN = 1000;

    /**
     * Maximum number of ports in a batch (chunk) when scanning a remote host.
     */
    public static final int CHUNK_PORTS_REMOTE_MAX = 6000;

    public PortScanner(PortScannerContext cxt) {
        this.invocationCommand = Objects.requireNonNull(cxt.getInvocationCommand(), "invocation command must be set");
        this.fileWriter = cxt.getNockerFileWriter();
        this.outputFormatter = Objects.requireNonNull(cxt.getOutputFormatter(), "formatter must be set");
        this.schedulerFactory = Objects.requireNonNull(cxt.getSchedulerFactory(), "scheduler factory must be set");
        this.timeout = cxt.getTimeout();
        this.concurrency = cxt.getConcurrency();
        this.sneak = cxt.isSyn();
        this.robust = cxt.isRobust();
    }

    @Scan
    public void scan(@Hosts List<String> hosts, @Port int port) {
        Map<PortScanReport, HostIdentity> reports = new LinkedHashMap<>();
        for (String host : hosts) {
            HostIdentity hostIdentity = getHostIdentity(host);
            if (hostIdentity != null) {
                PortScanReport report = singleHostAndSinglePortScan(hostIdentity, port);
                if (report != null) {
                    reports.put(report, hostIdentity);
                }
            }
        }
        for (Map.Entry<PortScanReport, HostIdentity> entry : reports.entrySet()) {
            triggerResponse(entry.getKey(), entry.getValue());
        }
    }

    @Scan
    // scan complete
    public void scanSingleHostAndSinglePort(@Host String host, @Port int port) {
        HostIdentity hostIdentity = getHostIdentity(host);
        if (hostIdentity != null) {
            PortScanReport report = singleHostAndSinglePortScan(hostIdentity, port);
            if (report != null) {
                triggerResponse(report, hostIdentity);
            }
        }
        // notify client otherwise
    }

    private PortScanReport singleHostAndSinglePortScan(HostIdentity hostIdentity, int port) {
        if (ObjectUtils.isNotEmpty(hostIdentity.getHostInet4Address())) {
            List<PortScanResult> result = submitTask(hostIdentity.getHostInet4Address(),
                    Collections.singletonList(port));
            return generatePortScanReportFromPortScanResults(result);
        } else {
            return null;
        }
    }

    /**
     * The code below is a simple round-robin strategy to determine schedulers
     * for tasks. However, there's certainly a better way to do this in more
     * as Nocker scales(Round Robin strategy here is fine). However, it may
     * make sense (if MAX_SCHEDULERS increase, hosts to scan increases or chunks
     * are host-independent) that a host-affine scheduling strategy is introduced.
     * In this case, tasks will be deterministically routed to schedulers based
     * on hostIdentity.
     * <p>
     * This ensures that all tasks with the same host are handled by the same
     * scheduler, improves locality, reduces contention, and simplifies the
     * aggregation of scan results (something currently slowing Nocker down).
     *<pre>
     *{@code
     * PortScanScheduler scheduler = schedulers.get(i % schedulers.size());
     *}
     *</pre>
     * Becomes:
     * <p>
     * <pre>
     *{@code
     * Math.abs(hostIdentity.hashCode()) % schedulers.size()
     *}
     * </pre>
     * @param hosts
     */
    @Scan
    public void scan(@Hosts List<String> hosts) {
        List<PortScanScheduler> schedulers = spawnSchedulers(hosts.size());
        Map<PortScanReport, HostIdentity> reports = new LinkedHashMap<>();
        for (int i = 0; i < hosts.size(); i++) {
            HostIdentity hostIdentity = getHostIdentity(hosts.get(i));
            PortScanScheduler scheduler = schedulers.get(i % schedulers.size());
            reports.put(singleHostScan(hostIdentity, scheduler), hostIdentity);
        }
        for (Map.Entry<PortScanReport, HostIdentity> entry : reports.entrySet()) {
            triggerResponse(entry.getKey(), entry.getValue());
        }
    }

    // BEWARE: this scans all valid ports on a single host
    // scan complete
    @Scan
    public void scan(@Host String host) {
        HostIdentity hostIdentity = getHostIdentity(host);
        PortScanScheduler scanScheduler = schedulerFactory.create();
        if (hostIdentity != null) {
            PortScanReport report = singleHostScan(hostIdentity, scanScheduler);
            if (report != null) {
                triggerResponse(report, hostIdentity);
            }
        }
        // notify client otherwise
    }

    private PortScanReport singleHostScan(HostIdentity hostIdentity, PortScanScheduler scheduler) {
        if (ObjectUtils.isNotEmpty(hostIdentity.getHostInet4Address())) {
            AtomicInteger taskCount = new AtomicInteger(0);
            int batchSize = getBatchSize(hostIdentity.getHostInet4Address());
            List<PortRange> chunks = getChunks(MIN_PORT, MAX_PORT, batchSize);
            fireInTheHole(scheduler, hostIdentity.getHostInet4Address(), chunks, taskCount);
            return scheduler.shutdownAndCollect(taskCount);
        }
        return null;
    }

    // any filtering such as : PortScannerUtil.sortStringListPortsToIntegerList(ports)
    // should report the filtered ports
    // allows range scan: nocker scan host=localhost ports=8080,8081,8082,8083,8084
    // scan logic complete
    @Scan
    public void scan(@Host String host, @Ports List<String> ports) {
        HostIdentity hostIdentity = getHostIdentity(host);
        if (hostIdentity == null) {
            // notify
            LOGGER.warn("Cannot scan nonexistent host: {}", host);
            return;
        }
        List<Integer> validPorts = PortScannerUtil.convertListOfPortStringsToIntegers(ports);
        if (ObjectUtils.isNotEmpty(hostIdentity.getHostInet4Address()) && ObjectUtils.isNotEmpty(validPorts)) {
            // local scans of ports less than MIN PORTS CURRENCY USAGE
            if (PortScannerUtil.isLocalHost(hostIdentity.getHostname()) && ports.size() < MIN_PORTS_CONCURRENCY_USAGE) {
                List<PortScanResult> results = submitTask(hostIdentity.getHostInet4Address(), validPorts);
                PortScanReport report = generatePortScanReportFromPortScanResults(results);
                triggerResponse(report, hostIdentity);
            } else {
                AtomicInteger taskCount = new AtomicInteger(0);
                int batchSize = getBatchSize(hostIdentity.getHostInet4Address());
                List<Integer> sortedPorts = PortScannerUtil.sortStringListPortsToIntegerList(ports);
                List<PortRange> chunks = getChunks(sortedPorts.get(0), sortedPorts.get(sortedPorts.size() - 1), batchSize);
                PortScanScheduler scanScheduler = schedulerFactory.create();
                fireInTheHole(scanScheduler, hostIdentity.getHostInet4Address(), chunks, taskCount);
                PortScanReport report = scanScheduler.shutdownAndCollect(taskCount);
                triggerResponse(report, hostIdentity);
            }
        }
    }

    // scan logic complete
    @Scan
    public void scan(@Host String host, @Ports PortWildcard ports) {
        HostIdentity hostIdentity = getHostIdentity(host);
        if (hostIdentity == null) {
            // notify
            LOGGER.warn("Cannot scan nonexistent host: {}", host);
            return;
        }
        if (ObjectUtils.isNotEmpty(hostIdentity.getHostInet4Address())) {
            int batchSize = getBatchSize(hostIdentity.getHostInet4Address());
            AtomicInteger taskCount = new AtomicInteger(0);
            List<PortRange> chunks = getChunks(ports.getLowPort(), ports.getHighPort(), batchSize);
            PortScanScheduler scanScheduler = schedulerFactory.create();
            fireInTheHole(scanScheduler, hostIdentity.getHostInet4Address(), chunks, taskCount);
            PortScanReport report = scanScheduler.shutdownAndCollect(taskCount);
            triggerResponse(report, hostIdentity);
        } else {
            PortScannerUtil.logInvalidHost(host);
        }
    }

    // add (CIDRWildcard hosts, List<String> ports)
    // add (CIDRWildcard hosts, PortWildcard ports)


    // too slow - possible performance boost processing both hosts & addresses concurrently
    // currently only processing ports concurrently, given a single hosts. too slow. This is
    // stupid slow, most likely coming from building the hostModels
    // scan logic complete
    @CIDRScan
    public void cidrScan(@Hosts CIDRWildcard hosts) {
        if (hosts.isValidCIDRWildcard()) {
            if (hosts.getOctets()[3] == 0) {
                hosts.incrementLastOctet();
            }
            Inet4Address hostAddress = PortScannerUtil.getHostInet4Address(hosts.getAddress());
            int batchSize = getBatchSize(hostAddress);
            List<PortRange> chunks = getChunks(MIN_PORT, MAX_PORT, batchSize);

            AtomicInteger taskCount = new AtomicInteger(0);
            PortScanScheduler scanScheduler = schedulerFactory.create();
            while (hosts.getOctets()[3] < 255) {
                Inet4Address address = PortScannerUtil.getHostInet4Address(hosts.getAddress());
                if (ObjectUtils.isNotEmpty(address)) {
                    fireInTheHole(scanScheduler, hostAddress, chunks, taskCount);
                }
                hosts.incrementLastOctet();
            }
            PortScanReport report = scanScheduler.shutdownAndCollect(taskCount);
            List<HostModel> hostModels = collectHostModels(scanScheduler, report.getResults());
            triggerResponse(report, hostModels);
        }
    }

    public InvocationCommand getInvocationCommand() {
        return invocationCommand;
    }

    public NockerFileWriter getFileWriter() {
        return fileWriter;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public int getTimeout() {
        return timeout;
    }

    private void fireInTheHole(PortScanScheduler scanScheduler, Inet4Address hostAddress, List<PortRange> chunks, AtomicInteger taskCount) {
        for (PortRange portRange : chunks) {
            submitTask(scanScheduler, hostAddress, portRange);
            taskCount.incrementAndGet();
        }
    }

    private void submitTask(PortScanScheduler scanScheduler, Inet4Address inet4Address, PortRange portRange) {
        if (sneak) {
            scanScheduler.submit(new PortScanSynTask(scanScheduler.getSchedulerId(), inet4Address, portRange,
                    sourcePortAllocator.getAndIncrement(), timeout));
        } else {
            scanScheduler.submit(new PortScanSynAckTask(scanScheduler.getSchedulerId(), inet4Address,
                    portRange, timeout));
        }
    }

    private void submitTask(PortScanScheduler scanScheduler, Inet4Address inet4Address, List<Integer> ports) {
        if (sneak) {
            scanScheduler.submit(new PortScanSynTask(scanScheduler.getSchedulerId(), inet4Address, ports,
                    sourcePortAllocator.getAndIncrement(), timeout));
        } else {
            scanScheduler.submit(new PortScanSynAckTask(scanScheduler.getSchedulerId(), inet4Address,
                    ports, timeout));
        }
    }

    private List<PortScanResult> submitTask(Inet4Address inet4Address, PortRange portRange) {
        if (sneak) {
            PortScanSynTask task = new PortScanSynTask(null, inet4Address, portRange, sourcePortAllocator.getAndIncrement(),
                    timeout);
            return task.call();
        } else {
            PortScanSynAckTask task = new PortScanSynAckTask(null, inet4Address, portRange, timeout);
            return task.call();
        }
    }

    private List<PortScanResult> submitTask(Inet4Address inet4Address, List<Integer> ports) {
        if (sneak) {
            PortScanSynTask task = new PortScanSynTask(null, inet4Address, ports, sourcePortAllocator.getAndIncrement(),
                    timeout);
            return task.call();
        } else {
            PortScanSynAckTask task = new PortScanSynAckTask(null, inet4Address, ports, timeout);
            return task.call();
        }
    }

    // update this: ports are grouped to correct host, but ports are out of order. For json output this matters.
    // for human-readable output, not so much because nocker should just report on OPEN,CLOSED,FILTERED ports.
    private List<HostModel> collectHostModels(PortScanScheduler scanScheduler, List<PortScanResult> portScanResults) {
        Map<Inet4Address, List<PortScanResult>> byHost = portScanResults.stream()
                .collect(Collectors.groupingBy(PortScanResult::getHostAddress));

        List<HostModel> hostModels = new ArrayList<>();
        for (Map.Entry<Inet4Address, List<PortScanResult>> entry : byHost.entrySet()) {
            Inet4Address host = entry.getKey();
            HostIdentity hostIdentity = getHostIdentity(host.getHostAddress());
            hostModels.add(responseWithHostModel(scanScheduler, entry.getValue(),
                    hostIdentity));
        }
        return hostModels;
    }

    private void triggerResponse(PortScanReport report, List<HostModel> batchHostResults) {
        if (robust) {
            doShowOutput(batchHostResults);
        } else {
            doShowOutput(report.getSummary());
        }
    }

    private void triggerResponse(PortScanReport report, HostIdentity hostIdentity) {
        if (robust) {
            HostModel hostModel = responseWithHostModel(report.getPortScanScheduler(), report.getResults(), hostIdentity);
            doShowOutput(hostModel);
        } else {
            doShowOutput(report.getSummary());
        }
    }

    private void triggerResponse(List<PortScanResult> results, HostIdentity hostIdentity) {
        HostModel hostModel = responseWithHostModel(results, hostIdentity);
        doShowOutput(hostModel);
    }

    private HostModel responseWithHostModel(PortScanScheduler scanScheduler, List<PortScanResult> results, HostIdentity hostIdentity) {
        return new HostModel.Builder()
                .schedulerId(scanScheduler.getSchedulerId())
                .hostIdentity(hostIdentity)
                .tasks(results)
                .durationMillis(scanScheduler.getDurationMillisBatch().orElse(0))
                .build();
    }

    private HostModel responseWithHostModel(List<PortScanResult> results, HostIdentity hostIdentity) {
        return new HostModel.Builder()
                .schedulerId(null)
                .hostIdentity(hostIdentity)
                .tasks(results)
                .durationMillis(0)
                .build();
    }

    private void writeToFile(ScanSummary scanSummary) {
        if (ObjectUtils.isNotEmpty(scanSummary)) {
            if (ObjectUtils.allNotNull(outputFormatter, fileWriter)) {
                outputFormatter.write(scanSummary, fileWriter.getPrintStream());
            }
        }
    }

    private void writeToFile(Object obj) {
        if (ObjectUtils.isNotEmpty(obj)) {
            if (ObjectUtils.allNotNull(outputFormatter, fileWriter)) {
                outputFormatter.write(obj, fileWriter.getPrintStream());
            }
        }
    }

    private void writeToFile(List<?> listObject) {
        if (ObjectUtils.isNotEmpty(listObject)) {
            if (ObjectUtils.allNotNull(outputFormatter, fileWriter)) {
                outputFormatter.write(listObject, fileWriter.getPrintStream());
            }
        }
    }

    private void doShowOutput(Object obj) {
        outputFormatter.write(obj, System.out);
        writeToFile(obj);
    }

    private void doShowOutput(List<?> listObject) {
        outputFormatter.write(listObject, System.out);
        writeToFile(listObject);
    }

    private void doShowOutput(ScanSummary scanSummary) {
        outputFormatter.write(scanSummary, System.out);
        writeToFile(scanSummary);
    }

    /**
     * Generates a {@code PortScanReport} object from the provided list of port scan
     * results along with additional scan metadata such as the invocation command.
     * This method processes the scan results to calculate the total number of open,
     * filtered, and closed ports, as well as builds a mapping of hostnames to their
     * corresponding open ports.
     *
     * @param results a list of {@code PortScanResult} objects representing
     *                the results of a port scan, including port states and
     *                associated host information
     * @return a {@link PortScanReport}
     */
    protected PortScanReport generatePortScanReportFromPortScanResults(List<PortScanResult> results) {
        int openPortsCount = 0;
        int filteredPortsCount = 0;
        int closedPortsCount = 0;
        int totalPortsScanned = 0;
        HashMap<String, Set<Integer>> openHostPorts = new HashMap<>();
        for (PortScanResult result : results) {
            int port = result.getPort();
            totalPortsScanned++;
            if (result.getState().equals(OPEN)) {
                openPortsCount++;
                openHostPorts.computeIfAbsent(result.getHostAddress().getHostName(), host -> new HashSet<>())
                        .add(port);
            } else if (result.getState().equals(FILTERED)) {
                filteredPortsCount++;
            } else {
                if (result.getState().equals(CLOSED)) {
                    closedPortsCount++;
                }
            }
        }
        ScanSummary scanSummary = new ScanSummary(0L,
                null,
                invocationCommand,
                0L,
                sumSequentialDuration(results),
                new AtomicInteger(openPortsCount),
                new AtomicInteger(filteredPortsCount),
                new AtomicInteger(closedPortsCount),
                new AtomicInteger(totalPortsScanned),
                openHostPorts
        );
        return new PortScanReport(null, results, scanSummary);
    }

    /**
     * Retrieves the identity details of a specified host by resolving
     * its IPv4 address, host address, and hostname. This method
     * creates a {@link HostIdentity} instance with the resolved values.
     *
     * @param host the hostname or IP address of the host to resolve
     * @return a {@link HostIdentity} instance containing the resolved
     * identity details of the specified host
     */
    private HostIdentity getHostIdentity(String host) {
        if (ObjectUtils.isNotEmpty(host)) {
            Inet4Address hostAddress = PortScannerUtil.getHostInet4Address(host);
            String hostAddressName = PortScannerUtil.getHostInet4AddressName(hostAddress.getHostAddress());
            if (ObjectUtils.isNotEmpty(hostAddress) && ObjectUtils.isNotEmpty(hostAddressName)) {
                return new HostIdentity.Builder()
                        .hostInet4Address(hostAddress)
                        .hostAddress(hostAddress.getHostAddress())
                        .hostname(hostAddressName)
                        .build();
            }
        }
        return null;
    }

    /**
     * Spawns a list of {@code PortScanScheduler} instances based
     * on the requested size, limited to a maximum number defined
     * by {@code MAX_SCHEDULERS}. The method dynamically creates
     * the schedulers using the factory method and caps the total
     * number of schedulers to avoid exceeding the pre-defined
     * limit.
     *
     * @param requestedSize the number of schedulers to spawn,
     *                     subject to the {@code MAX_SCHEDULERS}
     *                     limit
     * @return a list of {@code PortScanScheduler} instances,
     * with the size being the lesser of {@code requestedSize}
     * and the maximum allowed schedulers
     */
    protected List<PortScanScheduler> spawnSchedulers(int requestedSize) {
        int count = Math.min(requestedSize, MAX_SCHEDULERS);
        List<PortScanScheduler> schedulers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            schedulers.add(schedulerFactory.create());
        }
        return schedulers;
    }

    /**
     * Generates a list of {@code PortRange} objects representing sub-ranges
     * of ports between a given starting port and ending port, divided into
     * chunks of a specified batch size.
     *
     * @param startPort the starting port number (inclusive)
     * @param endPort the ending port number (inclusive), which must not be
     *                less than the starting port number
     * @param batchSize the maximum size of each chunk of ports
     * @return a list of {@code PortRange} objects, where each object represents
     *         a range of ports. The batch size of each range will not exceed
     *         {@code batchSize}, and all ranges together cover the full range
     *         from {@code startPort} to {@code endPort}.
     * @throws IllegalStateException if {@code endPort} is less than
     * {@code startPort}
     */
    protected List<PortRange> getChunks(int startPort, int endPort, int batchSize) {
        if (endPort < startPort) {
            throw new IllegalStateException("ending port can not be less than starting port");
        }
        int totalSize = (endPort - startPort) + startPort;
        if (batchSize >= totalSize) {
            return Collections.singletonList(new PortRange(startPort, endPort));
        }
        int remainder = totalSize % batchSize;
        int segments = remainder == 0 ? totalSize / batchSize : (totalSize / batchSize) + 1;
        List<PortRange> portRanges = new ArrayList<>();
        for (int i = 0; i < segments; i++) {
            int segmentEnd;
            if (startPort + batchSize - 1 >= endPort) {
                segmentEnd = endPort;
                portRanges.add(new PortRange(startPort, segmentEnd));
                break;
            }
            segmentEnd = startPort + batchSize - 1;
            portRanges.add(new PortRange(startPort, segmentEnd));
            startPort = segmentEnd + 1;
        }
        return portRanges;
    }

    /**
     * Calculates the total elapsed duration for a set of port scan
     * results that were executed sequentially.
     * <p>
     * This method assumes that each {@link PortScanResult} represents
     * a non-overlapping unit of work and that the scans were performed
     * one after another in a single execution context. Under these
     * conditions, the sum of individual durations is equivalent to
     * the wall-clock time observed by the caller.
     * <p>
     * <strong>Important:</strong> This method must <em>not</em> be used
     * for results produced by concurrent or parallel scans, as summing
     * overlapping durations will overstate the actual elapsed time.
     * For concurrent scans, wall-clock duration should be calculated
     * using the overall start and end timestamps instead.
     *
     * @param results the list of {@link PortScanResult} instances
     *                produced by a sequential scan
     * @return the total elapsed duration in milliseconds
     */
    protected long sumSequentialDuration(List<PortScanResult> results) {
        long total = 0L;
        for (PortScanResult result : results) {
            total += result.getDurationMillis();
        }
        return total;
    }

    private int getBatchSize(Inet4Address address) {
        return PortScannerUtil.isLocalHost(address.getHostAddress()) ? calculateLocalBatchSize() : calculateRemoteBatchSize();
    }

    private int calculateLocalBatchSize() {
        return (int) Math.ceil(Math.max(CHUNK_PORTS_LOCAL_MIN, Math.min(CHUNK_PORTS_LOCAL_MAX, TARGET_TASK_COMPLETION_LOCAL_MS / AVG_LOCAL_SCAN_MS)));
    }

    private int calculateRemoteBatchSize() {
        return (int) Math.ceil(Math.max(CHUNK_PORTS_REMOTE_MIN, Math.min(CHUNK_PORTS_REMOTE_MAX, TARGET_TASK_COMPLETION_REMOTE_MS / AVG_REMOTE_SCAN_MS)));
    }
}
