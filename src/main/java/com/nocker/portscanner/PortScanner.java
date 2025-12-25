package com.nocker.portscanner;

import com.nocker.Flag;
import com.nocker.portscanner.annotation.arguements.Host;
import com.nocker.portscanner.annotation.arguements.Hosts;
import com.nocker.portscanner.annotation.arguements.Port;
import com.nocker.portscanner.command.InvocationCommand;
import com.nocker.portscanner.annotation.arguements.Ports;
import com.nocker.portscanner.annotation.commands.CIDRScan;
import com.nocker.portscanner.annotation.commands.Scan;
import com.nocker.portscanner.scheduler.PortScanSynAckScheduler;
import com.nocker.portscanner.tasks.PortScanSynAckTask;
import com.nocker.portscanner.tasks.PortScanSynTask;
import com.nocker.portscanner.wildcard.CIDRWildcard;
import com.nocker.portscanner.wildcard.PortWildcard;
import com.nocker.writer.NockerFileWriter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateUtils;

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
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65536;

    // this max is random - justify this (or a higher/lower bound) with numbers
    private static final int MAX_PORTS_CONCURRENCY_USAGE = 100;
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int DEFAULT_CONCURRENCY = 100;

    private int timeout;
    private int concurrency;
    private final String outFilePath;
    private final InvocationCommand invocationCommand;
    private final NockerFileWriter fileWriter;

    public PortScanner(InvocationCommand invocationCommand, NockerFileWriter nockerFileWriter) {
        // instead of initializing these values - they should be recognized in
        // main and initialized in the constructor.
        this.invocationCommand = invocationCommand;
        this.fileWriter = nockerFileWriter;
        initTimeout();
        initConcurrency();
        this.outFilePath = initOutFile();
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
        InetAddress hostAddress = PortScannerUtil.getHostAddress(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            logScanningHostMessage(host, fileWriter);

            // refactor port scheduler logic into a method
            PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency);
            for (int port = MIN_PORT; port <= MAX_PORT; port++) {
                scanScheduler.submit(new PortScanSynAckTask(hostAddress, port, timeout));
            }
            // Scheduler will block until all task are complete, then attempt a graceful shutdown
            scanScheduler.shutdownAndWait();
            System.out.println("All ports scanned"); // extend this to report number of ports open
        }
    }

    @Scan
    public void scan(@Host String host, @Port int port) {
        InetAddress hostAddress = PortScannerUtil.getHostAddress(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            logSimpleHostWithPortScan(host, port, fileWriter);
            connectPortImmediate(hostAddress, port, fileWriter);
        } else {
            // should be sent to a logger
            System.out.println("Scanning Host: " + host + " Port: " + port);
            connectPortImmediate(hostAddress, port, fileWriter);
        }
    }

    // allows range scan: nocker scan host=localhost ports=8080,8081,8082,8083,8084
    @Scan
    public void scan(@Host String host, @Ports List<String> ports) {
        InetAddress hostAddress = PortScannerUtil.getHostAddress(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            logScanningHostMessage(host, fileWriter);
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
                for (String port : ports) {
                    if (PortScannerUtil.isValidPortNumber(port)) {
                        int p = PortScannerUtil.converPortToInteger(port);
                        scanScheduler.submit(new PortScanSynAckTask(hostAddress, p, timeout));
                    }
                }
                scanScheduler.shutdownAndWait();
                System.out.println("All ports scanned");
            }
        }
    }

    @Scan
    public void scan(@Host String host, @Ports PortWildcard ports) {
        Inet4Address inet4Address = PortScannerUtil.getHostInet4Address(host);
        if (ObjectUtils.isNotEmpty(inet4Address)) {
            // for now, will allocate a single SourcePortAllocator as a range of source ports
            SourcePortAllocator sourcePortAllocator = new SourcePortAllocator(MIN, MAX);
            PortScanSynAckScheduler scanScheduler = new PortScanSynAckScheduler(concurrency);
            int destinationPortLow = ports.getLowPort();
            int destinationPortHigh = ports.getHighPort();
            logScanningHostMessage(inet4Address.getHostAddress(), fileWriter);
            while (destinationPortLow <= destinationPortHigh) {
                scanScheduler.submit(new PortScanSynTask(inet4Address, destinationPortLow,
                        sourcePortAllocator.getAndIncrement(), timeout));
                destinationPortLow++;
            }
            scanScheduler.shutdownAndWait();
            System.out.println("All ports scanned");
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
                InetAddress address = PortScannerUtil.getHostAddress(hosts.getAddress());
                if (ObjectUtils.isNotEmpty(address)) {
                    logScanningHostMessage(hosts.getAddress(), fileWriter);
                    for (int port = MIN_PORT; port <= MAX_PORT; port++) {
                        scanScheduler.submit(new PortScanSynAckTask(address, port, timeout));
                    }
                }
                hosts.incrementLastOctet();
            }
            System.out.println("All ports scanned");
            scanScheduler.shutdownAndWait();
            System.out.println("Schedule closed.");
        }
    }

    public InvocationCommand getInvocationCommand() {
        return invocationCommand;
    }

    public String getOutFilePath() {
        return outFilePath;
    }

    private void connectPortImmediate(InetAddress hostAddress, int port, NockerFileWriter writer) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostAddress, port), timeout);
            if (writer != null) {
                writer.write("[" + getTime() + "] " + "Port: " + socket.getPort() + " is open");
            } else {
                System.out.println("Port: " + socket.getPort() + " is open");
            }
        } catch (IOException e) {
            if (writer != null) {
                writer.write("[" + getTime() + "] " + "Port: " + port + " is closed");
            } else {
                System.out.println("Port: " + port + " is closed");
            }
        }
    }

    private void initTimeout() {
        Map<String, String> flags = invocationCommand.getCommandLineInput().getFlags();
        int proposedTimeout = Integer.parseInt(flags.getOrDefault(Flag.TIMEOUT.getFullName(),
                String.valueOf(DEFAULT_TIMEOUT)));
        if (proposedTimeout >= 1000 && proposedTimeout <= 10000) {
            this.timeout = proposedTimeout;
        } else {
            this.timeout = DEFAULT_TIMEOUT;
        }
    }

    private void initConcurrency() {
        Map<String, String> flags = invocationCommand.getCommandLineInput().getFlags();
        int proposedConcurrency = Integer.parseInt(flags.getOrDefault(Flag.CONCURRENCY.getFullName(),
                String.valueOf(DEFAULT_CONCURRENCY)));
        if (proposedConcurrency >= 2 && proposedConcurrency <= 300) {
            this.concurrency = proposedConcurrency;
        } else {
            this.concurrency = DEFAULT_CONCURRENCY;
        }
    }

    private String initOutFile() {
        Map<String, String> flags = invocationCommand.getCommandLineInput().getFlags();
        return flags.getOrDefault(Flag.OUT.getFullName(), null);
    }

    private void logScanningHostMessage(String host, NockerFileWriter writer) {
        if (writer != null) {
            writer.write("[" + getTime() + "] " + "Scanning Host: " + host);
            writer.write("[" + getTime() + "] " + "Config Settings: (timeout=" + timeout + ", concurrency=" + concurrency + "ms" + ")");
        }
    }

    private void logSimpleHostWithPortScan(String host, int port, NockerFileWriter writer) {
        writer.write("[" + getTime() + "] " + "Scanning Host: " + host + " Port: " + port);
    }

    private LocalDateTime getTime() {
        return DateUtils.toLocalDateTime(new Date(), TimeZone.getDefault());
    }
}
