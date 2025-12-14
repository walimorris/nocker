package com.nocker.commands;

import com.nocker.Flag;
import com.nocker.annotations.arguements.Host;
import com.nocker.annotations.arguements.Port;
import com.nocker.annotations.commands.Scan;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;

public class CommandService {
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65536;

    private static final int DEFAULT_TIMEOUT = 5000;

    private int timeout;

    public CommandService() {}

    public CommandService(InvocationCommand invocationCommand) {
        initTimeout(invocationCommand);
    }

    @Scan
    public void scan(@Host String host) {
        InetAddress hostAddress = getHostAddress(host);
        if (ObjectUtils.isNotEmpty(hostAddress)) {
            System.out.println("Scanning Host: " + host);
            int port = MIN_PORT;
            while (port <= MAX_PORT) {
                connectPort(hostAddress, port);
                port++;
            }
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

    private void connectPort(InetAddress hostAddress, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostAddress, port), timeout);
            System.out.println("Port: " + socket.getPort() + " is open");
        } catch (IOException e) {
            // do nothing
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
        int proposedTimeout = Integer.parseInt(flags.getOrDefault(Flag.TIMEOUT.getFullName(), String.valueOf(DEFAULT_TIMEOUT)));
        if (proposedTimeout >= 1000 && proposedTimeout <= 10000) {
            this.timeout = proposedTimeout;
        } else {
            this.timeout = DEFAULT_TIMEOUT;
        }
    }
}
