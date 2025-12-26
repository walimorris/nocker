package com.nocker.portscanner.tasks;

import com.nocker.portscanner.PortScanResult;
import com.nocker.portscanner.PortState;
import com.nocker.portscanner.packet.Ipv4TcpSynPacket;
import com.nocker.portscanner.packet.TcpSynSegment;
import org.apache.logging.log4j.core.util.UuidUtil;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.Callable;

public class PortScanSynTask implements PortScanTask, Callable<PortScanResult>, Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortScanSynTask.class);

    private final Inet4Address destinationHost;
    private final int destinationPort;
    private final int sourcePort;
    private final int timeout;
    private final UUID schedulerId;
    private final UUID taskId = UuidUtil.getTimeBasedUuid();
    // add a tll

    public PortScanSynTask(UUID schedulerId, Inet4Address destinationHost, int destinationPort, int sourcePort,
                           int timeout) {
        this.schedulerId = schedulerId;
        this.destinationHost = destinationHost;
        this.destinationPort = destinationPort;
        this.sourcePort = sourcePort;
        this.timeout = timeout;
    }

    @Override
    public PortScanResult call() {
        LOGGER.info("Starting task in sneak mode: {}", this);
        long start = System.currentTimeMillis();
        PortState finalState = PortState.FILTERED;

        TcpSynSegment tcpSynSegment = new TcpSynSegment((short) sourcePort, (short) destinationPort, destinationHost);
        Ipv4TcpSynPacket ipv4TcpSynPacket = generateIpv4TcpSynPacketFromTcpSynSegment(tcpSynSegment);
        PcapHandle pcapHandle = openHandle(ipv4TcpSynPacket);
        try {
            pcapHandle.setFilter(generateFilter(), BpfProgram.BpfCompileMode.OPTIMIZE);
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
        return new PortScanResult(
                schedulerId,
                taskId,
                destinationHost,
                destinationPort,
                finalState,
                duration
        );
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

    protected String generateFilter() {
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
    public String toString() {
        return "PortScanSynTask{" +
                "taskId=" + taskId +
                ", schedulerId=" + schedulerId +
                ", destinationHost=" + destinationHost.getHostAddress() +
                ", destinationPort=" + destinationPort +
                ", sourcePort=" + sourcePort +
                ", timeout=" + timeout +
                '}';
    }
}
