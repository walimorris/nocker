package com.nocker.portscanner.tasks;

import org.apache.logging.log4j.core.util.UuidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.*;
import java.util.UUID;

public class PortScanSynAckTask implements PortScanTask, Runnable, Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortScanSynAckTask.class);

    private final InetAddress host;
    private final int port;
    private final int timeout;
    private final UUID schedulerId;
    private final UUID taskId = UuidUtil.getTimeBasedUuid();

    public PortScanSynAckTask(UUID schedulerId, InetAddress host, int port, int timeout) {
        this.schedulerId = schedulerId;
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        LOGGER.info("Starting task in loud mode: {}", this);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            LOGGER.info("Port: {} is open", + socket.getPort());
        } catch (IOException e) {
            // do nothing
        }
    }

    @Override
    public String getTaskIdText() {
        return taskId.toString();
    }

    @Override
    public String getSchedulerIdText() {
        return schedulerId.toString();
    }

    @Override
    public String toString() {
        return "PortScanSynAckTask{" +
                "taskId=" + taskId +
                ", schedulerId=" + schedulerId +
                ", host=" + host.getHostAddress() +
                ", port=" + port +
                ", timeout=" + timeout +
                '}';
    }
}
