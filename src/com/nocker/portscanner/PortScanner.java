package com.nocker.portscanner;

import com.nocker.CIDRWildcard;
import com.nocker.portscanner.annotations.arguements.Host;
import com.nocker.portscanner.annotations.arguements.Hosts;
import com.nocker.portscanner.annotations.arguements.Port;
import com.nocker.InvocationCommand;
import com.nocker.portscanner.annotations.arguements.Ports;
import com.nocker.portscanner.annotations.commands.CIDRScan;
import com.nocker.portscanner.annotations.commands.Scan;
import com.nocker.portscanner.schedulers.PortScanScheduler;
import com.nocker.portscanner.tasks.PortScanTask;
import org.apache.commons.lang3.ObjectUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class PortScanner {
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65536;

    // this max is random - justify this (or a higher/lower bound) with numbers
    private static final int MAX_PORTS_CONCURRENCY_USAGE = 100;
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int DEFAULT_CONCURRENCY = 100;

    private int timeout;
    private int concurrency;

    public PortScanner() {}

    public PortScanner(InvocationCommand invocationCommand) {
        initTimeout(invocationCommand);
        initConcurrency(invocationCommand);
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
            logScanningHostMessage(host);

            // refactor port scheduler logic into a method
            PortScanScheduler scanScheduler = new PortScanScheduler(concurrency);
            for (int port = MIN_PORT; port <= MAX_PORT; port++) {
                scanScheduler.submit(new PortScanTask(hostAddress, port, timeout));
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
            System.out.println("Scanning Host: " + host + " Port: " + port);
            connectPortImmediate(hostAddress, port);
        }
    }

    // allows range scan: nocker scan host=localhost ports=8080,8081,8082,8083,8084
    @Scan
    public void scan(@Host String host, @Ports List<String> ports) {
        InetAddress hostAddress = PortScannerUtil.getHostAddress(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            logScanningHostMessage(host);
            if (ports.size() < MAX_PORTS_CONCURRENCY_USAGE) {
                for (String port : ports) {
                    if (isValidPortNumber(port)) {
                        connectPortImmediate(hostAddress, converPortToInteger(port));
                    } else {
                        logInvalidPortNumber(port);
                    }
                }
            } else {
                // refactor port scanning logic into a method
                PortScanScheduler scanScheduler = new PortScanScheduler(concurrency);
                for (String port : ports) {
                    if (isValidPortNumber(port)) {
                        int p = converPortToInteger(port);
                        scanScheduler.submit(new PortScanTask(hostAddress, p, timeout));
                    }
                }
                scanScheduler.shutdownAndWait();
                System.out.println("All ports scanned");
            }
        }
    }

    @Scan
    public void scan(@Host String host, @Ports String ports) {}


    // too slow - possible performance boost processing both hosts & addresses concurrently
    // currently only processing ports concurrently, given a single hosts. too slow.
    @CIDRScan
    public void cidrScan(@Hosts CIDRWildcard hosts) {
        if (hosts.isValidCIDRWildcard()) {
            if (hosts.getOctets()[3] == 0) {
                hosts.incrementLastOctet();
            }
            PortScanScheduler scanScheduler = new PortScanScheduler(concurrency);
            while (hosts.getOctets()[3] < 255) {
                InetAddress address = PortScannerUtil.getHostAddress(hosts.getAddress());
                if (ObjectUtils.isNotEmpty(address)) {
                    logScanningHostMessage(hosts.getAddress());
                    for (int port = MIN_PORT; port <= MAX_PORT; port++) {
                        scanScheduler.submit(new PortScanTask(address, port, timeout));
                    }
                }
                hosts.incrementLastOctet();
            }
            System.out.println("All ports scanned");
            scanScheduler.shutdownAndWait();
            System.out.println("Schedule closed.");
        }
    }

    private void connectPortImmediate(InetAddress hostAddress, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostAddress, port), timeout);
            System.out.println("Port: " + socket.getPort() + " is open");
        } catch (IOException e) {
            System.out.println("Port: " + port + " is closed");
        }
    }

    private void initTimeout(InvocationCommand invocationCommand) {
        Map<String, String> flags = invocationCommand.commandLineInput().getFlags();
        int proposedTimeout = Integer.parseInt(flags.getOrDefault(Flag.TIMEOUT.getFullName(),
                String.valueOf(DEFAULT_TIMEOUT)));
        if (proposedTimeout >= 1000 && proposedTimeout <= 10000) {
            this.timeout = proposedTimeout;
        } else {
            this.timeout = DEFAULT_TIMEOUT;
        }
    }

    private void initConcurrency(InvocationCommand invocationCommand) {
        Map<String, String> flags = invocationCommand.commandLineInput().getFlags();
        int proposedConcurrency = Integer.parseInt(flags.getOrDefault(Flag.CONCURRENCY.getFullName(),
                String.valueOf(DEFAULT_CONCURRENCY)));
        if (proposedConcurrency >= 2 && proposedConcurrency <= 300) {
            this.concurrency = proposedConcurrency;
        } else {
            this.concurrency = DEFAULT_CONCURRENCY;
        }
    }

    // invalid port will return 0
    private int converPortToInteger(String p) {
        try {
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            logInvalidPortNumber(p);
            return 0;
        }
    }

    private boolean isValidPortNumber(String p) {
        return isValidPortNumber(converPortToInteger(p));
    }

    private boolean isValidPortNumber(int p) {
        return (p >= MIN_PORT && p <= MAX_PORT);
    }

    private void logInvalidPortNumber(String port) {
        System.out.println("invalid port: " + port);
    }

    private void logScanningHostMessage(String host) {
        System.out.println("Scanning Host: " + host);
        System.out.println("config settings: timeout: " + timeout + " concurrency: " + concurrency);
    }
}
