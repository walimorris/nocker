package com.nocker.commands;

import com.nocker.annotations.arguements.Host;
import com.nocker.annotations.arguements.Port;
import com.nocker.annotations.commands.Scan;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class CommandService {

    @Scan
    public void scanPort(@Host String host, @Port int port) throws IOException {
        System.out.println("Scanning Host: " + host + " Port" + port);

        InetAddress hostAddress = null;
        try {
            hostAddress = isLocalHost(host) ? InetAddress.getLocalHost() : InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.out.println("Host not found: " + host);
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostAddress, port), 2000);
            if (socket.isConnected()) {
                System.out.println("Port: " + socket.getPort() + " is open");
            } else {
                System.out.println("Port: " + socket.getPort() + " is closed");
            }
        }
    }

    @Scan
    public void scanPort(@Port int port) {

    }

    private boolean isLocalHost(String host) {
        if (StringUtils.isNotBlank(host)) {
            host = host.toLowerCase();
            return host.contains("localhost");
        }
        return false;
    }
}
