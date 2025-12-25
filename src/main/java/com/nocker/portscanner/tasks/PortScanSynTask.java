package com.nocker.portscanner.tasks;

import com.nocker.portscanner.packet.Ipv4TcpSynPacket;
import com.nocker.portscanner.packet.TcpSynSegment;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;

import java.net.*;

public class PortScanSynTask implements Runnable {
    private final Inet4Address destinationHost;
    private final int destinationPort;
    private final int sourcePort;
    private final int timeout;
    // add a tll

    public PortScanSynTask(Inet4Address destinationHost, int destinationPort, int sourcePort,
                           int timeout) {
        this.destinationHost = destinationHost;
        this.destinationPort = destinationPort;
        this.sourcePort = sourcePort;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        System.out.println("scanning port: " + destinationPort + "[" + this.getClass().getName() + "]");
        TcpSynSegment tcpSynSegment = new TcpSynSegment((short) sourcePort, (short) destinationPort, destinationHost);
        Ipv4TcpSynPacket ipv4TcpSynPacket = generateIpv4TcpSynPacketFromTcpSynSegment(tcpSynSegment);
        PcapHandle pcapHandle = openHandle(ipv4TcpSynPacket);
        String filter = generateFilter();
        try {
            pcapHandle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);
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
                    System.out.println("Port Open: " + destinationPort);
                    break;
                }
                if (header.getRst()) {
                    System.out.println("Port Closed: " + destinationPort);
                    break;
                }
            }
            System.out.println("Port Filtered: " + destinationPort);
        } catch (Exception e) {
            // add more robust logging
            System.out.println("Error handling packet transmission: " + e.getMessage());
        } finally {
            if (pcapHandle.isOpen()) {
                pcapHandle.close();
            }
        }
    }

    protected Ipv4TcpSynPacket generateIpv4TcpSynPacketFromTcpSynSegment(TcpSynSegment tcpSynSegment) {
        Ipv4TcpSynPacket ipv4TcpSynPacket;
        try {
            ipv4TcpSynPacket = new Ipv4TcpSynPacket(tcpSynSegment, destinationHost);
        } catch (PcapNativeException | SocketException e) {
            throw new RuntimeException("Failed to initialize IPv4 SYN packet", e);
        }
        return ipv4TcpSynPacket;
    }

    protected PcapHandle openHandle(Ipv4TcpSynPacket ipv4TcpSynPacket) {
        PcapHandle pcapHandle = null;
        try {
            String nif = ipv4TcpSynPacket.getNetworkInterface().getName();
            System.out.println("Attempting to initialize PcapHandle with interface: " + nif);
            pcapHandle = new PcapHandle.Builder(nif)
                    .snaplen(65536)
                    .promiscuousMode(PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS)
                    .immediateMode(true)
                    .timeoutMillis(10)
                    .build();
            System.out.println("PcapHandle successfully initialized.");
        } catch (PcapNativeException e) {
            System.out.println("Failed to initialize PcapHandle. Exception: " + e.getMessage());
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
}
