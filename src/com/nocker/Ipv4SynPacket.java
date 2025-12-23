package com.nocker;

import com.nocker.portscanner.TcpSynSegment;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

public class Ipv4SynPacket {
    private final TcpSynSegment tcpSynSegment;
    private final Inet4Address sourceAddress;
    private final Inet4Address destinationAddress;

    public Ipv4SynPacket(TcpSynSegment tcpSynSegment, Inet4Address destinationAddress) {
        this.tcpSynSegment = tcpSynSegment;
        this.sourceAddress = initSourceAddress();
        this.destinationAddress = destinationAddress;
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

    private Inet4Address initSourceAddress() {
        Inet4Address sourceAddr = null;
        try {
            List<PcapNetworkInterface> networkInterfaces = Pcaps.findAllDevs();
            Collections.shuffle(networkInterfaces);

            for (PcapNetworkInterface nif : networkInterfaces) {
                System.out.println("interface: " + nif.getName() + "[" + nif.getDescription() + "]");
                for (PcapAddress address : nif.getAddresses()) {
                    InetAddress addr = address.getAddress();
                    if (addr instanceof Inet4Address) {
                        System.out.println("IPv4 Address: " + addr.getHostAddress());
                        sourceAddr = (Inet4Address) addr;
                        break;
                    }
                }
                if (sourceAddr != null) {
                    break;
                }
            }
            if (sourceAddr == null) {
                System.out.println("Not able to locate source Ipv4 address");
            }
        } catch (PcapNativeException e) {
            System.out.println("Error locating source Ipv4 address.");
            return null;
        }
        return sourceAddr;
    }
}
