package com.nocker.portscanner.report;

import com.nocker.portscanner.PortState;

import java.net.Inet4Address;
import java.util.UUID;

public final class PortScanResult {
    private final UUID schedulerId;
    private final UUID taskId;
    private final Inet4Address hostAddress;
    private final int port;
    private final PortState state;
    private final long durationMillis;

    public PortScanResult(UUID schedulerId, UUID taskId, Inet4Address host, int port, PortState state,
                          long durationMillis) {
        this.schedulerId = schedulerId;
        this.taskId = taskId;
        this.hostAddress = host;
        this.port = port;
        this.state = state;
        this.durationMillis = durationMillis;
    }

    public UUID getSchedulerId() {
        return schedulerId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public Inet4Address getHostAddress() {
        return hostAddress;
    }

    public int getPort() {
        return port;
    }

    public PortState getState() {
        return state;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}
