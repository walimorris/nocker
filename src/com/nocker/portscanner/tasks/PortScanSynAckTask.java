package com.nocker.portscanner.tasks;

import java.io.IOException;
import java.net.*;

public class PortScanSynAckTask implements Runnable {
    private final InetAddress host;
    private final int port;
    private final int timeout;

    public PortScanSynAckTask(InetAddress host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            System.out.println("Port: " + socket.getPort() + " is open");
        } catch (IOException e) {
            // do nothing
        }
    }
}
