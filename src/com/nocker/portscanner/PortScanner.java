package com.nocker.portscanner;

import com.nocker.portscanner.annotations.arguements.Host;
import com.nocker.portscanner.annotations.arguements.Hosts;
import com.nocker.portscanner.annotations.arguements.Port;
import com.nocker.InvocationCommand;
import com.nocker.portscanner.annotations.commands.Scan;
import com.nocker.portscanner.schedulers.PortScanScheduler;
import com.nocker.portscanner.tasks.PortScanTask;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class PortScanner {
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65536;

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
        InetAddress hostAddress = getHostAddress(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            System.out.println("Scanning Host: " + host);
            System.out.println("config settings: timeout: " + timeout + " concurrency: " + concurrency);

            PortScanScheduler scanScheduler = new PortScanScheduler(concurrency);
            for (int port = MIN_PORT; port <= MAX_PORT; port++) {
                scanScheduler.submit(new PortScanTask(hostAddress, port, timeout));
            }
            // call graceful shutdown of scan scheduler. All port scan tasks have
            // been given to the scheduler. Scheduler will block until all task are
            // complete, then attempt a graceful shutdown
            scanScheduler.shutdownAndWait();
            System.out.println("All ports scanned"); // extend this to report number of ports open
        }
    }

    @Scan
    public void scan(@Host String host, @Port int port) {
        InetAddress hostAddress = getHostAddress(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            System.out.println("Scanning Host: " + host + " Port: " + port);
            connectPortImmediate(hostAddress, port);
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

    private InetAddress getHostAddress(String host) {
        InetAddress hostAddress = null;
        try {
            hostAddress = isLocalHost(host) ? InetAddress.getLocalHost() : InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.out.println("Host not found: " + host);
        }
        return hostAddress;
    }

    private boolean isLocalHost(String host) {
        if (StringUtils.isNotBlank(host)) {
            host = host.toLowerCase();
            return host.contains("localhost");
        }
        return false;
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
}
