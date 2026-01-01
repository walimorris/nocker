package com.nocker.portscanner.tasks;

import com.nocker.portscanner.PortScanResult;
import com.nocker.portscanner.PortScannerUtil;
import com.nocker.portscanner.PortState;
import org.apache.logging.log4j.core.util.UuidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

// review: duration times - because of update
public class PortScanSynAckTask implements PortScanTask, Callable<List<PortScanResult>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortScanSynAckTask.class);

    private final Inet4Address host;
    private final List<Integer> ports;
    private final PortRange portRange;
    private final int timeout;
    private final UUID schedulerId;
    private final UUID taskId = UuidUtil.getTimeBasedUuid();

    private static final long serialVersionUID = 1L;

    public PortScanSynAckTask(UUID schedulerId, Inet4Address host, List<Integer> ports, int timeout) {
        this.schedulerId = schedulerId;
        this.host = host;
        this.ports = ports;
        this.portRange = null;
        this.timeout = timeout;
    }

    public PortScanSynAckTask(UUID schedulerId, Inet4Address host, PortRange portRange, int timeout) {
        this.schedulerId = schedulerId;
        this.host = host;
        this.ports = null;
        this.portRange = portRange;
        this.timeout = timeout;
    }

    @Override
    public List<PortScanResult> call() {
        long start = System.currentTimeMillis();
        List<PortScanResult> results = new ArrayList<>();
        if (portRange != null && ports == null) {
            int lowDestinationPort = portRange.getLow();
            int highDestinationPort = portRange.getHigh();
            while (lowDestinationPort <= highDestinationPort) {
                iteratePort(results, lowDestinationPort, start);
                lowDestinationPort++;
            }
        } else {
            if (ports != null && portRange == null) {
                for (int destinationPort : ports) {
                    iteratePort(results, destinationPort, start);
                }
            }
        }
        return results;
    }

    private void iteratePort(List<PortScanResult> results, int destinationPort, long start) {
        PortState state;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, destinationPort), timeout);
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
        results.add(new PortScanResult(
                schedulerId,
                taskId,
                host,
                destinationPort,
                state,
                duration
        ));
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
    public PortRange getDestinationPortRange() {
        return PortScannerUtil.getPortRange(ports);
    }

    @Override
    public String toString() {
        return "PortScanSynAckTask{" +
                "taskId=" + taskId +
                ", schedulerId=" + schedulerId +
                ", host=" + host.getHostAddress() +
                ", ports=" + getDestinationPortRange() +
                ", timeout=" + timeout +
                '}';
    }
}
