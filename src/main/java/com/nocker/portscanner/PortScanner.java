package com.nocker.portscanner;

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
import com.nocker.portscanner.scheduler.PortScanSynAckScheduler;
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

    public PortScanner(InvocationCommand invocationCommand,
                       NockerFileWriter nockerFileWriter,
                       OutputFormatter outputFormatter,
                       int timeout,
                       int concurrency,
                       boolean sneak,
                       boolean robust) {
        this.invocationCommand = invocationCommand;
        this.fileWriter = nockerFileWriter;
        this.outputFormatter = outputFormatter;
        this.timeout = timeout >= TIME_OUT_LOW_LIMIT && timeout <= TIME_OUT_HIGH_LIMIT ? timeout : DEFAULT_TIMEOUT;
        this.concurrency = concurrency >= 2 && concurrency <= 300 ? concurrency : DEFAULT_CONCURRENCY;
        this.sneak = sneak;
        this.robust = robust;
    }

    // needs updating now that multiple host scans are supported
    @Scan
    public void scan(@Hosts List<String> hosts, @Port int port) {
        for (String host : hosts) {
            scanSingleHostAndSinglePort(host, port);
        }
    }

    @Scan
    public void scan(@Hosts List<String> hosts) {
        List<PortScanScheduler> schedulers = spawnSchedulers(hosts.size(), invocationCommand);
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
        PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency, invocationCommand);
        PortScanReport report = singleHostScan(hostIdentity, scanScheduler);
        if (report != null) {
            triggerResponse(report, hostIdentity);
        }
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

    @Scan
    // scan complete
    public void scanSingleHostAndSinglePort(@Host String host, @Port int port) {
        HostIdentity hostIdentity = getHostIdentity(host);
        long start = System.nanoTime();
        if (ObjectUtils.isNotEmpty(hostIdentity.getHostInet4Address())) {
            List<PortScanResult> result = submitTask(hostIdentity.getHostInet4Address(),
                    Collections.singletonList(port));
            long stop = System.nanoTime();
            ScanSummary summary = generateScanSummaryFromPortScanResults(result, invocationCommand, start, stop);
            triggerResponse(new PortScanReport(null, result, summary), hostIdentity);
        } else {
            PortScannerUtil.logInvalidHost(host);
        }
    }

    // any filtering such as : PortScannerUtil.sortStringListPortsToIntegerList(ports)
    // should report the filtered ports
    // allows range scan: nocker scan host=localhost ports=8080,8081,8082,8083,8084
    // scan logic complete
    @Scan
    public void scan(@Host String host, @Ports List<String> ports) {
        HostIdentity hostIdentity = getHostIdentity(host);
        List<Integer> validPorts = PortScannerUtil.convertListOfPortStringsToIntegers(ports);
        if (ObjectUtils.isNotEmpty(hostIdentity.getHostInet4Address()) && ObjectUtils.isNotEmpty(validPorts)) {
            // local scans of ports less than MIN PORTS CURRENCY USAGE
            if (PortScannerUtil.isLocalHost(hostIdentity.getHostname()) && ports.size() < MIN_PORTS_CONCURRENCY_USAGE) {
                List<PortScanResult> results = submitTask(hostIdentity.getHostInet4Address(), validPorts);
                // TODO: build a ScanSummary and use with TriggerResponse()
                triggerResponse(results, hostIdentity);
            } else {
                AtomicInteger taskCount = new AtomicInteger(0);
                int batchSize = getBatchSize(hostIdentity.getHostInet4Address());
                List<Integer> sortedPorts = PortScannerUtil.sortStringListPortsToIntegerList(ports);
                List<PortRange> chunks = getChunks(sortedPorts.get(0), sortedPorts.get(sortedPorts.size() - 1), batchSize);
                PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency, invocationCommand);
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
        if (ObjectUtils.isNotEmpty(hostIdentity.getHostInet4Address())) {
            int batchSize = getBatchSize(hostIdentity.getHostInet4Address());
            AtomicInteger taskCount = new AtomicInteger(0);
            List<PortRange> chunks = getChunks(ports.getLowPort(), ports.getHighPort(), batchSize);
            PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency, invocationCommand);
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
            PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency, invocationCommand);
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

    private ScanSummary generateScanSummaryFromPortScanResults(List<PortScanResult> results, InvocationCommand command, long start, long stop) {
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
        return new ScanSummary(start,
                null,
                command,
                stop,
                new AtomicInteger(openPortsCount),
                new AtomicInteger(filteredPortsCount),
                new AtomicInteger(closedPortsCount),
                new AtomicInteger(totalPortsScanned),
                openHostPorts
        );
    }

    private HostIdentity getHostIdentity(String host) {
        Inet4Address hostAddress = PortScannerUtil.getHostInet4Address(host);
        String hostAddressName = PortScannerUtil.getHostInet4AddressName(hostAddress.getHostAddress());
        return new HostIdentity.Builder()
                .hostInet4Address(hostAddress)
                .hostAddress(hostAddress.getHostAddress())
                .hostname(hostAddressName)
                .build();
    }

    /**
     * Creates and initializes a list of {@code PortScanScheduler} instances
     * based on the requested size, subject to the maximum allowable
     * schedulers.
     *
     * @param requestedSize the desired number of schedulers to be created
     * @param invocationCommand the {@code InvocationCommand} instance that
     *                          contains the command-line input, target
     *                          method, and its arguments for configuring
     *                          each scheduler
     * @return a list of {@code PortScanScheduler} instances, with the size
     * determined by the smaller value between {@code requestedSize} and the
     * {@link PortScanner#MAX_SCHEDULERS}
     */
    private List<PortScanScheduler> spawnSchedulers(int requestedSize, InvocationCommand invocationCommand) {
        int count = Math.min(requestedSize, MAX_SCHEDULERS);
        List<PortScanScheduler> schedulers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            schedulers.add(new PortScanSynAckScheduler(invocationCommand));
        }
        return schedulers;
    }

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
