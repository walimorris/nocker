package com.nocker.portscanner;

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
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

import static com.nocker.portscanner.SourcePortAllocator.MAX;
import static com.nocker.portscanner.SourcePortAllocator.MIN;

public class PortScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortScanner.class);

    private final int timeout;
    private final int concurrency;
    private final InvocationCommand invocationCommand;
    private final NockerFileWriter fileWriter;
    private final boolean sneak;

    public static final int MIN_PORT = 1;
    public static final int MAX_PORT = 65536;

    // this max is random - justify this (or a higher/lower bound) with numbers
    public static final int MAX_PORTS_CONCURRENCY_USAGE = 100;
    public static final int DEFAULT_TIMEOUT = 5000;
    public static final int DEFAULT_CONCURRENCY = 100;
    // for now, will allocate a single SourcePortAllocator as a range of source ports
    public static final SourcePortAllocator sourcePortAllocator = new SourcePortAllocator(MIN, MAX);

    public PortScanner(InvocationCommand invocationCommand,
                       NockerFileWriter nockerFileWriter,
                       int timeout,
                       int concurrency,
                       boolean sneak) {
        this.invocationCommand = invocationCommand;
        this.fileWriter = nockerFileWriter;
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
            writeToFileScanningHostMessage(host, fileWriter);

            // refactor port scheduler logic into a method
            PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency);
            logSchedulerStarted(scanScheduler);
            for (int port = MIN_PORT; port <= MAX_PORT; port++) {
                submitTask(scanScheduler, hostAddress, port);
            }
            // Scheduler will block until all task are complete, then attempt a graceful shutdown
            scanScheduler.shutdownAndWait();
            logAllPortsScanned(); // extend this to report number of ports open
        }
    }

    @Scan
    public void scan(@Host String host, @Port int port) {
        InetAddress hostAddress = PortScannerUtil.getHostAddress(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            writeToFileSimpleHostWithPortScan(host, port, fileWriter);
            connectPortImmediate(hostAddress, port, fileWriter);
        } else {
            // should be sent to a logger
            LOGGER.info("Scanning Host: {} Port: {}", host, port);
            connectPortImmediate(hostAddress, port, fileWriter);
        }
    }

    // allows range scan: nocker scan host=localhost ports=8080,8081,8082,8083,8084
    @Scan
    public void scan(@Host String host, @Ports List<String> ports) {
        Inet4Address hostAddress = PortScannerUtil.getHostInet4Address(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            writeToFileScanningHostMessage(host, fileWriter);
            if (ports.size() < MAX_PORTS_CONCURRENCY_USAGE) {
                for (String port : ports) {
                    if (PortScannerUtil.isValidPortNumber(port)) {
                        connectPortImmediate(hostAddress, PortScannerUtil.converPortToInteger(port), fileWriter);
                    } else {
                        PortScannerUtil.logInvalidPortNumber(port);
                    }
                }
            } else {
                // refactor port scanning logic into a method
                PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency);
                logSchedulerStarted(scanScheduler);
                for (String port : ports) {
                    if (PortScannerUtil.isValidPortNumber(port)) {
                        int p = PortScannerUtil.converPortToInteger(port);
                        submitTask(scanScheduler, hostAddress, p);
                    }
                }
                scanScheduler.shutdownAndWait();
                logAllPortsScanned();
            }
        }
    }

    @Scan
    public void scan(@Host String host, @Ports PortWildcard ports) {
        Inet4Address inet4Address = PortScannerUtil.getHostInet4Address(host);
        if (ObjectUtils.isNotEmpty(inet4Address)) {
            PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency);
            logSchedulerStarted(scanScheduler);
            int destinationPortLow = ports.getLowPort();
            int destinationPortHigh = ports.getHighPort();
            writeToFileScanningHostMessage(inet4Address.getHostAddress(), fileWriter);
            while (destinationPortLow <= destinationPortHigh) {
                submitTask(scanScheduler, inet4Address, destinationPortLow);
                destinationPortLow++;
            }
            scanScheduler.shutdownAndWait();
            logAllPortsScanned();
            logSchedulerClosed(scanScheduler);
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
            logSchedulerStarted(scanScheduler);
            while (hosts.getOctets()[3] < 255) {
                Inet4Address address = PortScannerUtil.getHostInet4Address(hosts.getAddress());
                if (ObjectUtils.isNotEmpty(address)) {
                    writeToFileScanningHostMessage(hosts.getAddress(), fileWriter);
                    for (int port = MIN_PORT; port <= MAX_PORT; port++) {
                        submitTask(scanScheduler, address, port);
                    }
                }
                hosts.incrementLastOctet();
            }
            logAllPortsScanned();
            scanScheduler.shutdownAndWait();
            logSchedulerClosed(scanScheduler);
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

    // introduce loud or sneak option
    private void connectPortImmediate(InetAddress hostAddress, int port, NockerFileWriter writer) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostAddress, port), timeout);
            if (writer != null) {
                writer.write("[" + getTime() + "] " + "Port: " + socket.getPort() + " is open");
            } else {
                LOGGER.info("Port: {} is open", socket.getPort());
            }
        } catch (IOException e) {
            if (writer != null) {
                writer.write("[" + getTime() + "] " + "Port: " + port + " is closed");
            } else {
                LOGGER.info("Port: {} is closed", port);
            }
        }
    }

    private void writeToFileScanningHostMessage(String host, NockerFileWriter writer) {
        if (writer != null) {
            writer.write("[" + getTime() + "] " + "Scanning Host: " + host);
            writer.write("[" + getTime() + "] " + "Config Settings: (timeout=" + timeout + ", concurrency=" + concurrency + "ms" + ")");
        }
    }

    private void writeToFileSimpleHostWithPortScan(String host, int port, NockerFileWriter writer) {
        writer.write("[" + getTime() + "] " + "Scanning Host: " + host + " Port: " + port);
    }

    private void logAllPortsScanned() {
        LOGGER.info("All ports scanned");
    }

    private void logSchedulerClosed(PortScanScheduler scheduler) {
        LOGGER.info("Stopped Scheduler: {}", scheduler.toString());
    }

    private void logSchedulerStarted(PortScanScheduler scheduler) {
        LOGGER.info("Started Scheduler: {}", scheduler.toString());
    }

    private LocalDateTime getTime() {
        return DateUtils.toLocalDateTime(new Date(), TimeZone.getDefault());
    }
}
