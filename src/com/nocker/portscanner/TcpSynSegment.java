package com.nocker.portscanner;

import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.TcpPort;

public class TcpSynSegment {
    private final TcpPort sourcePort;
    private final TcpPort destinationPort;

    private final static boolean IS_SYN_SCAN = true; // sends syn without completing full TCP 3-way
    private final static int SEQ_NUMBER = 100; // TCP initial sequence number that's acknowledged
    private final static int ACK_NUMBER = 0;   // ACK field in tcp header - no ack for syn scan
    private final static short WINDOW = (short) 65535; // receivable byte size
    private final static boolean DO_CHECKSUM_AT_BUILD = true; // pcap computes checksum
    private final static boolean DO_LENGTH_AT_BUILD = true;   // pcap computes data offset

    private final static String TCP_PORT_SOURCE_NAME = "source_port";
    private final static String TCP_PORT_DESTINATION_NAME = "destination_port";

    public TcpSynSegment(short sourcePort, short destinationPort) {
        this.sourcePort = new TcpPort(sourcePort, TCP_PORT_SOURCE_NAME);
        this.destinationPort = new TcpPort(destinationPort, TCP_PORT_DESTINATION_NAME);
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
}
