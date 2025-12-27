package com.nocker.portscanner;

import com.nocker.cli.formatter.OutputFormatter;
import com.nocker.portscanner.annotation.arguements.Host;
import com.nocker.portscanner.annotation.arguements.Hosts;
import com.nocker.portscanner.annotation.arguements.Port;
import com.nocker.portscanner.command.InvocationCommand;
import com.nocker.portscanner.annotation.arguements.Ports;
import com.nocker.portscanner.annotation.commands.CIDRScan;
import com.nocker.portscanner.annotation.commands.Scan;
import com.nocker.portscanner.scheduler.PortScanScheduler;
import com.nocker.portscanner.scheduler.PortScanSynAckScheduler;
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

import static com.nocker.portscanner.SourcePortAllocator.MAX;
import static com.nocker.portscanner.SourcePortAllocator.MIN;

public class PortScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortScanner.class);

    private final int timeout;
    private final int concurrency;
    private final InvocationCommand invocationCommand;
    private final NockerFileWriter fileWriter;
    private final OutputFormatter outputFormatter;
    private final boolean sneak;

    public static final int MIN_PORT = 1;
    public static final int MAX_PORT = 65536;

    // this max is random - justify this (or a higher/lower bound) with numbers
    public static final int MAX_PORTS_CONCURRENCY_USAGE = 100;
    public static final int DEFAULT_TIMEOUT = 500;
    public static final int DEFAULT_CONCURRENCY = 100;
    // for now, will allocate a single SourcePortAllocator as a range of source ports
    public static final SourcePortAllocator sourcePortAllocator = new SourcePortAllocator(MIN, MAX);

    public PortScanner(InvocationCommand invocationCommand,
                       NockerFileWriter nockerFileWriter,
                       OutputFormatter outputFormatter,
                       int timeout,
                       int concurrency,
                       boolean sneak) {
        this.invocationCommand = invocationCommand;
        this.fileWriter = nockerFileWriter;
        this.outputFormatter = outputFormatter;
        this.timeout = timeout >= 1000 && timeout <= 10000 ? timeout : DEFAULT_TIMEOUT;
        this.concurrency = concurrency >= 2 && concurrency <= 300 ? concurrency : DEFAULT_CONCURRENCY;
        this.sneak = sneak;
    }

    @Scan
    public void scan(@Hosts List<String> hosts, @Port int port) {
        for (String host : hosts) {
            scan(host, port);
        }
    }

    @Scan
    public void scan(@Hosts List<String> hosts) {
        for (String host : hosts) {
            scan(host);
        }
    }

    @Scan
    public void scan(@Host String host) {
        Inet4Address hostAddress = PortScannerUtil.getHostInet4Address(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency);
            for (int port = MIN_PORT; port <= MAX_PORT; port++) {
                submitTask(scanScheduler, hostAddress, port);
            }
            List<PortScanResult> results = scanScheduler.shutdownAndCollect(PortScanResult.class);
            doShowOutput(results);
        }
    }

    @Scan
    public void scan(@Host String host, @Port int port) {
        Inet4Address hostAddress = PortScannerUtil.getHostInet4Address(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            PortScanResult result = submitTask(hostAddress, port);
            doShowOutput(result);
        } else {
            // should write to standard out for user
            LOGGER.info("Cannot scan. Invalid host: {}", host);
        }
    }

    // allows range scan: nocker scan host=localhost ports=8080,8081,8082,8083,8084
    @Scan
    public void scan(@Host String host, @Ports List<String> ports) {
        Inet4Address hostAddress = PortScannerUtil.getHostInet4Address(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            if (ports.size() < MAX_PORTS_CONCURRENCY_USAGE) {
                List<PortScanResult> results = new ArrayList<>();
                for (String port : ports) {
                    if (PortScannerUtil.isValidPortNumber(port)) {
                        PortScanResult result = submitTask(hostAddress, PortScannerUtil.converPortToInteger(port));
                        results.add(result);
                    } else {
                        PortScannerUtil.logInvalidPortNumber(port);
                    }
                }
                doShowOutput(results);
            } else {
                // refactor port scanning logic into a method
                PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency);
                for (String port : ports) {
                    if (PortScannerUtil.isValidPortNumber(port)) {
                        int p = PortScannerUtil.converPortToInteger(port);
                        submitTask(scanScheduler, hostAddress, p);
                    }
                }
                List<PortScanResult> results = scanScheduler.shutdownAndCollect(PortScanResult.class);
                doShowOutput(results);
            }
        }
    }

    @Scan
    public void scan(@Host String host, @Ports PortWildcard ports) {
        Inet4Address inet4Address = PortScannerUtil.getHostInet4Address(host);
        if (ObjectUtils.isNotEmpty(inet4Address)) {
            PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency);
            int destinationPortLow = ports.getLowPort();
            int destinationPortHigh = ports.getHighPort();
            while (destinationPortLow <= destinationPortHigh) {
                submitTask(scanScheduler, inet4Address, destinationPortLow);
                destinationPortLow++;
            }
            List<PortScanResult> results = scanScheduler.shutdownAndCollect(PortScanResult.class);
            doShowOutput(results);
        } else {
            PortScannerUtil.logInvalidHost(host);
        }
    }


    // too slow - possible performance boost processing both hosts & addresses concurrently
    // currently only processing ports concurrently, given a single hosts. too slow.
    @CIDRScan
    public void cidrScan(@Hosts CIDRWildcard hosts) {
        if (hosts.isValidCIDRWildcard()) {
            if (hosts.getOctets()[3] == 0) {
                hosts.incrementLastOctet();
            }
            PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency);
            while (hosts.getOctets()[3] < 255) {
                Inet4Address address = PortScannerUtil.getHostInet4Address(hosts.getAddress());
                if (ObjectUtils.isNotEmpty(address)) {
                    for (int port = MIN_PORT; port <= MAX_PORT; port++) {
                        submitTask(scanScheduler, address, port);
                    }
                }
                hosts.incrementLastOctet();
            }
            List<PortScanResult> results = scanScheduler.shutdownAndCollect(PortScanResult.class);
            doShowOutput(results);
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

    private void submitTask(PortScanScheduler scanScheduler, Inet4Address inet4Address, int port) {
        if (sneak) {
            scanScheduler.submit(new PortScanSynTask(scanScheduler.getSchedulerId(), inet4Address, port,
                    sourcePortAllocator.getAndIncrement(), timeout));
        } else {
            scanScheduler.submit(new PortScanSynAckTask(scanScheduler.getSchedulerId(), inet4Address,
                    port, timeout));
        }
    }

    private PortScanResult submitTask(Inet4Address inet4Address, int port) {
        if (sneak) {
            PortScanSynTask task = new PortScanSynTask(null, inet4Address, port, sourcePortAllocator.getAndIncrement(),
                    timeout);
            return task.call();
        } else {
            PortScanSynAckTask task = new PortScanSynAckTask(null, inet4Address, port, timeout);
            return task.call();
        }
    }

    private void writeToFile(PortScanResult portScanResult) {
        if (ObjectUtils.isNotEmpty(portScanResult)) {
            if (ObjectUtils.allNotNull(outputFormatter, fileWriter)) {
                outputFormatter.write(portScanResult, fileWriter.getPrintStream());
            }
        }
    }

    private void writeToFile(List<PortScanResult> portScanResult) {
        if (ObjectUtils.isNotEmpty(portScanResult)) {
            if (ObjectUtils.allNotNull(outputFormatter, fileWriter)) {
                outputFormatter.write(portScanResult, fileWriter.getPrintStream());
            }
        }
    }

    private void doShowOutput(List<PortScanResult> results) {
        outputFormatter.write(results, System.out);
        writeToFile(results);
    }

    private void doShowOutput(PortScanResult result) {
        outputFormatter.write(result, System.out);
        writeToFile(result);
    }
}
