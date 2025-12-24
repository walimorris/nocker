package com.nocker.portscanner.tasks;

import com.nocker.Ipv4TcpSynPacket;
import com.nocker.portscanner.TcpSynSegment;
import org.apache.commons.lang3.ObjectUtils;
import org.pcap4j.core.*;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.TcpPort;

import java.net.Inet4Address;

public class PortScanSynTask implements Runnable {
    private final Inet4Address destinationHost;
    private final int destinationPort;
    private final int sourcePort;
    private final int timeout;
    // add a tll

    private static final PcapNetworkInterface.PromiscuousMode MODE = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS;

    public PortScanSynTask(Inet4Address destinationHost, int destinationPort, int sourcePort,
                           int timeout) {
        this.destinationHost = destinationHost;
        this.destinationPort = destinationPort;
        this.sourcePort = sourcePort;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        System.out.println("scanning port: " + destinationPort);
        TcpSynSegment tcpSynSegment = new TcpSynSegment((short) sourcePort, (short) destinationPort);
        Ipv4TcpSynPacket ipv4TcpSynPacket = new Ipv4TcpSynPacket(tcpSynSegment, destinationHost);
        try (PcapHandle handle = ipv4TcpSynPacket.getNetworkInterface().openLive(65536, MODE, timeout)) {
            handle.sendPacket(ipv4TcpSynPacket.createIpv4Packet());
            while (true) {
                PcapPacket packet = handle.getNextPacket();
                if (ObjectUtils.isEmpty(packet)) { // timeout no response
                    System.out.println("Port Filtered: " + destinationPort);
                    break;
                }
                TcpPacket tcpPacket = packet.get(TcpPacket.class);
                if (tcpPacket == null) {
                    continue;
                }
                TcpPacket.TcpHeader header = tcpPacket.getHeader();

                // is this for current syn?
                TcpPort descPort = new TcpPort((short) destinationPort, "destination_port");
                TcpPort srcPort = new TcpPort((short) sourcePort, "source_port");
                if (!header.getSrcPort().equals(descPort) || !header.getDstPort().equals(srcPort)) {
                    continue;
                }

                if (header.getSyn() && header.getAck()) {
                    System.out.println("Port Open: " + destinationPort);
                    break;
                }
                if (header.getRst()) {
                    System.out.println("Port Closed: " + destinationPort);
                    break;
                }
            }
        } catch (PcapNativeException e) {
            System.out.println("Error opening channel on network interface: " + ipv4TcpSynPacket.getNetworkInterface().getName());
        } catch (NotOpenException e) {
            System.out.println("Error sending packet due to closed or unknown network interface: " + ipv4TcpSynPacket.getNetworkInterface().getName());
        }
    }
}
