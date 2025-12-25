package com.nocker.portscanner.packet;

import com.nocker.portscanner.PortScannerUtil;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Rfc791Tos;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.IpVersion;

import java.net.*;

public class Ipv4TcpSynPacket {
    private final TcpSynSegment tcpSynSegment;
    private final Inet4Address sourceAddress;
    private final Inet4Address destinationAddress;
    private final PcapNetworkInterface networkInterface;

    private final static IpVersion IP_VERSION = IpVersion.IPV4;
    private final static IpV4Rfc791Tos IPV4_RFC = IpV4Rfc791Tos.newInstance((byte) 0);
    private final static byte TTL = (byte) 64;
    private final static IpNumber PROTOCOL = IpNumber.TCP;


    public Ipv4TcpSynPacket(TcpSynSegment tcpSynSegment, Inet4Address destinationAddress) throws PcapNativeException, SocketException {
        this.tcpSynSegment = tcpSynSegment;
        this.destinationAddress = destinationAddress;
        this.sourceAddress = PortScannerUtil.resolveSourceIpAddress(destinationAddress);
        this.networkInterface = PortScannerUtil.resolveNetworkInterfaceFromSourceIp(sourceAddress);
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
}
