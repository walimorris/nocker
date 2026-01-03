package com.nocker.portscanner.tasks;

import com.nocker.portscanner.report.PortScanResult;
import com.nocker.portscanner.PortScannerUtil;
import com.nocker.portscanner.PortState;
import com.nocker.portscanner.packet.Ipv4TcpSynPacket;
import com.nocker.portscanner.packet.TcpSynSegment;
import org.apache.logging.log4j.core.util.UuidUtil;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

// review: duration times - because of update
public class PortScanSynTask implements PortScanTask, Callable<List<PortScanResult>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortScanSynTask.class);

    private final Inet4Address destinationHost;
    private final List<Integer> destinationPorts;
    private final PortRange destinationPortRange;
    private final int sourcePort;
    private final int timeout;
    private final UUID schedulerId;
    private final UUID taskId = UuidUtil.getTimeBasedUuid();
    // add a tll

    private static final long serialVersionUID = 1L;

    public PortScanSynTask(UUID schedulerId, Inet4Address destinationHost, List<Integer> destinationPorts, int sourcePort,
                           int timeout) {
        this.schedulerId = schedulerId;
        this.destinationHost = destinationHost;
        this.destinationPorts = destinationPorts;
        this.destinationPortRange = null;
        this.sourcePort = sourcePort;
        this.timeout = timeout;
    }

    public PortScanSynTask(UUID schedulerId, Inet4Address destinationHost, PortRange destinationPortRange, int sourcePort,
                           int timeout) {
        this.schedulerId = schedulerId;
        this.destinationHost = destinationHost;
        this.destinationPorts = null;
        this.destinationPortRange = destinationPortRange;
        this.sourcePort = sourcePort;
        this.timeout = timeout;
    }

    @Override
    public List<PortScanResult> call() {
        long start = System.currentTimeMillis();
        List<PortScanResult> results = new ArrayList<>();
        if (destinationPortRange != null && destinationPorts == null) {
            int lowDestinationPort = destinationPortRange.getLow();
            int highDestinationPort = destinationPortRange.getHigh();
            while (lowDestinationPort <= highDestinationPort) {
                iteratePort(results, lowDestinationPort, start);
                lowDestinationPort++;
            }
        } else {
            if (destinationPorts != null && destinationPortRange == null) {
                for (int destinationPort : destinationPorts) {
                    iteratePort(results, destinationPort, start);
                }
            }
        }
        return results;
    }

    private void iteratePort(List<PortScanResult> ongoingResults, int destinationPort, long start) {
        PortState finalState = PortState.FILTERED;
        TcpSynSegment tcpSynSegment = new TcpSynSegment((short) sourcePort, (short) destinationPort, destinationHost);
        Ipv4TcpSynPacket ipv4TcpSynPacket = generateIpv4TcpSynPacketFromTcpSynSegment(tcpSynSegment);
        PcapHandle pcapHandle = openHandle(ipv4TcpSynPacket);
        try {
            pcapHandle.setFilter(generateFilter(destinationPort), BpfProgram.BpfCompileMode.OPTIMIZE);
            pcapHandle.sendPacket(ipv4TcpSynPacket.createIpv4Packet());
            long deadline = System.currentTimeMillis() + timeout;
            while (System.currentTimeMillis() < deadline) {
                Packet packet = pcapHandle.getNextPacket();
                if (packet == null) {
                    continue;
                }
                TcpPacket tcpPacket = packet.get(TcpPacket.class);
                if (tcpPacket == null) {
                    continue;
                }
                TcpPacket.TcpHeader header = tcpPacket.getHeader();
                if (header.getSyn() && header.getAck()) {
                    finalState = PortState.OPEN;
                    break;
                }
                if (header.getRst()) {
                    finalState = PortState.CLOSED;
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Scan error on transmission: {}:{} - {}", destinationHost,
                    destinationPort, e.getMessage());
        } finally {
            if (pcapHandle.isOpen()) {
                pcapHandle.close();
            }
        }
        long duration = System.currentTimeMillis() - start;
        ongoingResults.add(new PortScanResult(
                schedulerId,
                taskId,
                destinationHost,
                destinationPort,
                finalState,
                duration
        ));
    }

    protected Ipv4TcpSynPacket generateIpv4TcpSynPacketFromTcpSynSegment(TcpSynSegment tcpSynSegment) {
        Ipv4TcpSynPacket ipv4TcpSynPacket;
        try {
            ipv4TcpSynPacket = new Ipv4TcpSynPacket(tcpSynSegment, destinationHost);
        } catch (PcapNativeException | SocketException e) {
            LOGGER.error("Failed to initialize IPv4 Syn Packet: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize IPv4 SYN packet", e);
        }
        return ipv4TcpSynPacket;
    }

    protected PcapHandle openHandle(Ipv4TcpSynPacket ipv4TcpSynPacket) {
        PcapHandle pcapHandle = null;
        try {
            String nif = ipv4TcpSynPacket.getNetworkInterface().getName();
            LOGGER.info("Attempting to initialize PcapHandle with interface: {}", nif);
            pcapHandle = new PcapHandle.Builder(nif)
                    .snaplen(65536)
                    .promiscuousMode(PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS)
                    .immediateMode(true)
                    .timeoutMillis(10)
                    .build();
            LOGGER.info("PcapHandle successfully initialized.");
        } catch (PcapNativeException e) {
            LOGGER.error("Failed to initialize PcapHandle: {}", e.getMessage());
        }
        return pcapHandle;
    }

    protected String generateFilter(int destinationPort) {
        StringBuilder filter = new StringBuilder();
        filter.append("tcp and src host ").append(destinationHost.getHostAddress());
        filter.append(" and src port ").append(destinationPort);
        filter.append(" and dst port ").append(sourcePort);
        filter.append(" and (tcp[tcpflags] & (tcp-syn|tcp-ack|tcp-rst) != 0)");
        return filter.toString();
    }

    public UUID getTaskId() {
        return taskId;
    }

    @Override
    public String getTaskIdText() {
        return taskId.toString();
    }

    public UUID getSchedulerId() {
        return schedulerId;
    }

    @Override
    public String getSchedulerIdText() {
        return schedulerId.toString();
    }

    @Override
    public PortRange getDestinationPortRange() {
        return PortScannerUtil.getPortRange(destinationPorts);
    }

    @Override
    public String toString() {
        return "PortScanSynTask{" +
                "taskId=" + taskId +
                ", schedulerId=" + schedulerId +
                ", destinationHost=" + destinationHost.getHostAddress() +
                ", destinationPorts=" + getDestinationPortRange() +
                ", sourcePort=" + sourcePort +
                ", timeout=" + timeout +
                '}';
    }
}
