package com.nocker.portscanner;

import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.TcpPort;

public class TcpSynSegment {
    private final TcpPort sourcePort;
    private final TcpPort destinationPort;

    /**
     * Allocates a range of ephemeral ports to the underlying TcpSynSegment. Port ranges
     * may need to consider other objects that may also use a range allocation, as to
     * not conflict and cause syn scans to use the same source-destination port pairs.
     */
    private final SourcePortAllocator sourcePortAllocator;

    private final boolean IS_SYN_SCAN = true; // sends syn without completing full TCP 3-way
    private final int SEQ_NUMBER = 100; // TCP initial sequence number that's acknowledged
    private final int ACK_NUMBER = 0;   // ACK field in tcp header - no ack for syn scan
    private final short WINDOW = (short) 65535; // receivable byte size
    private final boolean DO_CHECKSUM_AT_BUILD = true; // pcap computes checksum
    private final boolean DO_LENGTH_AT_BUILD = true;   // pcap computes data offset

    private final String TCP_PORT_SOURCE_NAME = "source_port";
    private final String TCP_PORT_DESTINATION_NAME = "destination_port";

    public TcpSynSegment(short destinationPort, SourcePortAllocator sourcePortAllocator) {
        this.destinationPort = new TcpPort(destinationPort, TCP_PORT_DESTINATION_NAME);
        this.sourcePortAllocator = sourcePortAllocator;
        this.sourcePort = new TcpPort((short) sourcePortAllocator.getAndIncrement(), TCP_PORT_SOURCE_NAME);
    }

    public TcpPacket createTcpSynSegment() {
        return new TcpPacket.Builder()
                .srcPort(sourcePort)
                .dstPort(destinationPort)
                .syn(IS_SYN_SCAN)
                .window(WINDOW)
                .correctChecksumAtBuild(DO_CHECKSUM_AT_BUILD)
                .correctLengthAtBuild(DO_LENGTH_AT_BUILD)
                .sequenceNumber(SEQ_NUMBER)
                .acknowledgmentNumber(ACK_NUMBER)
                .build();
    }

    public TcpPort getSourcePort() {
        return sourcePort;
    }

    public TcpPort getDestinationPort() {
        return destinationPort;
    }

    public SourcePortAllocator getSourcePortAllocator() {
        return sourcePortAllocator;
    }
}
