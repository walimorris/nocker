package com.nocker.portscanner.tasks;

import com.nocker.portscanner.PortScanResult;
import com.nocker.portscanner.PortState;
import org.apache.logging.log4j.core.util.UuidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.Callable;

public class PortScanSynAckTask implements PortScanTask, Callable<PortScanResult>, Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortScanSynAckTask.class);

    private final Inet4Address host;
    private final int port;
    private final int timeout;
    private final UUID schedulerId;
    private final UUID taskId = UuidUtil.getTimeBasedUuid();

    public PortScanSynAckTask(UUID schedulerId, Inet4Address host, int port, int timeout) {
        this.schedulerId = schedulerId;
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public PortScanResult call() {
        LOGGER.info("Starting task in loud mode: {}", this);
        long start = System.currentTimeMillis();
        PortState state;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            state = PortState.OPEN;
        }
        catch (SocketTimeoutException e) {
            state = PortState.FILTERED;
        }
        catch (ConnectException e) {
            state = PortState.CLOSED;
        }
        catch (IOException e) {
            state = PortState.ERROR;
        }
        long duration = System.currentTimeMillis() - start;
        return new PortScanResult(
                schedulerId,
                taskId,
                host,
                port,
                state,
                duration
        );
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
