package com.nocker;

import com.nocker.portscanner.TcpSynSegment;
import org.apache.commons.lang3.ObjectUtils;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Rfc791Tos;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.IpVersion;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

public class Ipv4TcpSynPacket {
    private final TcpSynSegment tcpSynSegment;
    private Inet4Address sourceAddress;
    private final Inet4Address destinationAddress;
    private PcapNetworkInterface networkInterface;

    private final static IpVersion IP_VERSION = IpVersion.IPV4;
    private final static IpV4Rfc791Tos IPV4_RFC = IpV4Rfc791Tos.newInstance((byte) 0);
    private final static byte TTL = (byte) 64;
    private final static IpNumber PROTOCOL = IpNumber.TCP;


    public Ipv4TcpSynPacket(TcpSynSegment tcpSynSegment, Inet4Address destinationAddress) {
        this.tcpSynSegment = tcpSynSegment;
        this.destinationAddress = destinationAddress;
        initSourceAddress(); // maybe add retry here
    }

    public IpV4Packet createIpv4Packet() {
        TcpPacket tcpPacketSegment = tcpSynSegment.createTcpSynSegment();
        return new IpV4Packet.Builder()
                .version(IP_VERSION)
                .tos(IPV4_RFC)
                .ttl(TTL)
                .protocol(PROTOCOL)
                .srcAddr(sourceAddress)
                .dstAddr(destinationAddress)
                .payloadBuilder(tcpPacketSegment.getBuilder())
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .build();
    }

    public TcpSynSegment getTcpSynSegment() {
        return tcpSynSegment;
    }

    public Inet4Address getSourceAddress() {
        return sourceAddress;
    }

    public Inet4Address getDestinationAddress() {
        return destinationAddress;
    }

    public PcapNetworkInterface getNetworkInterface() { return networkInterface; }

    private void initSourceAddress() {
        try {
            List<PcapNetworkInterface> networkInterfaces = Pcaps.findAllDevs();
            Collections.shuffle(networkInterfaces);

            for (PcapNetworkInterface nif : networkInterfaces) {
                System.out.println("interface: " + nif.getName() + "[" + nif.getDescription() + "]");
                for (PcapAddress address : nif.getAddresses()) {
                    InetAddress addr = address.getAddress();
                    if (addr instanceof Inet4Address) {
                        System.out.println("IPv4 Address: " + addr.getHostAddress());
                        networkInterface = nif;
                        sourceAddress = (Inet4Address) addr;
                        break;
                    }
                }
                if (sourceAddress != null) {
                    break;
                }
            }
            if (sourceAddress == null) {
                System.out.println("Not able to locate source Ipv4 address");
            }
        } catch (PcapNativeException e) {
            System.out.println("Error locating source Ipv4 address.");
        }
        if (ObjectUtils.anyNull(sourceAddress, networkInterface)) {
            throw new RuntimeException("Cannot create Ipv4TcpSynPacket with unknown interface or sourceAddress");
        }
    }
}
