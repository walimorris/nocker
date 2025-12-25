package com.nocker.portscanner;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Arrays;

public class PortScannerUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortScannerUtil.class);

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65536;

    private PortScannerUtil() {
        // unable to init util
    }

    public static Integer[] convertToIntegerArray(String[] objects) {
        return Arrays.stream(objects)
                .map(Integer::parseInt)
                .toArray(Integer[]::new);
    }

    public static String[] convertToStringArray(Integer[] objects) {
        return Arrays.stream(objects)
                .map(String::valueOf)
                .toArray(String[]::new);
    }

    public static String stringifyArray(Integer[] objects) {
        return String.join(".", convertToStringArray(objects));
    }

    public static String stringifyArray(String[] objects) {
        return String.join(".", objects);
    }

    public static InetAddress getHostAddress(String host) {
        InetAddress hostAddress = null;
        try {
            hostAddress = isLocalHost(host) ? InetAddress.getLocalHost() : InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            LOGGER.error("Fatality, unknown host [{}]: {}", host, e.getMessage(), e);
        }
        return hostAddress;
    }

    public static Inet4Address getHostInet4Address(String host) {
        Inet4Address hostAddress = null;
        try {
            hostAddress = isLocalHost(host) ? (Inet4Address) Inet4Address.getLocalHost() : (Inet4Address) Inet4Address.getByName(host);
        } catch (UnknownHostException e) {
            LOGGER.error("Fatality, unknown host [{}]: {}", host, e.getMessage(), e);
        }
        return hostAddress;
    }

    private static boolean isLocalHost(String host) {
        if (StringUtils.isNotBlank(host)) {
            host = host.toLowerCase();
            return host.contains("localhost");
        }
        return false;
    }

    /**
     * Utilizing a Datagram Socket connect method only sets the default destination and locks
     * the socket to that peer, and most importantly resolves routing locally. This chooses
     * an outbound interface, the source IP bound to that interface, and importantly does not
     * have any handshake behavior. It should be noted, Care should be taken to ensure that a
     * connected datagram socket is not shared with untrusted code. When a socket is connected,
     * receive and send will not perform any security checks on incoming and outgoing packets,
     * other than matching the packet's and the socket's address and port.
     *
     * @see DatagramSocket#connect(SocketAddress)
     * @param destinationAddress {@link Inet4Address}
     *
     * @return source {@link Inet4Address}
     */
    public static Inet4Address resolveSourceIpAddress(Inet4Address destinationAddress) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(destinationAddress, 53);
            InetAddress localAddress = socket.getLocalAddress();
            if (localAddress instanceof Inet4Address) {
                return (Inet4Address) localAddress;
            }
            LOGGER.error("Resolved non-IPv4 source address: {} ", destinationAddress);
            throw new IllegalStateException("must be valid IPv4 address");
        } catch (Exception e) {
            LOGGER.error("Failed to resolve source IP address from destination address [{}] via routing: ",
                    destinationAddress);
            throw new RuntimeException("Failed to resolve source IP address via routing.");
        }
    }

    /**
     * Gets the network interface from the given source address. If the list of network interface
     * addresses contains the source address (source address being source of transmission), then
     * it can be guaranteed that the current network interface is our source of communication.
     *
     * @param sourceAddress {@link Inet4Address} source of transmission
     *
     * @return {@link PcapNetworkInterface}
     * @throws {@link PcapNativeException}
     */
    public static PcapNetworkInterface resolveNetworkInterfaceFromSourceIp(InetAddress sourceAddress) throws PcapNativeException {
        for (PcapNetworkInterface nif : Pcaps.findAllDevs()) {
            for (PcapAddress currentAddress : nif.getAddresses()) {
                if (ObjectUtils.isEmpty(currentAddress.getAddress())) {
                    continue;
                }
                if (sourceAddress.equals(currentAddress.getAddress())) {
                    LOGGER.info("Source Address derived from network interface [{}, {}]",
                            nif.getName(), nif.getDescription());
                    return nif;
                }
            }
        }
        return null;
    }

    // invalid port will return 0
    public static int converPortToInteger(String p) {
        try {
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            logInvalidPortNumber(p);
            return 0;
        }
    }

    public static boolean isValidPortNumber(String p) {
        return isValidPortNumber(converPortToInteger(p));
    }

    public static boolean isValidPortNumber(int p) {
        return (p >= MIN_PORT && p <= MAX_PORT);
    }

    public static boolean allValidPortNumbers(String... ports) {
        for (String port : ports) {
            if (!isValidPortNumber(port)) {
                return false;
            }
        }
        return true;
    }

    public static boolean allIsValidPortNumbers(int... ports) {
        for (int port : ports) {
            if (!isValidPortNumber(port)) {
                return false;
            }
        }
        return true;
    }

    public static void logInvalidPortNumber(String port) {
        LOGGER.warn("invalid port: {}", port);
    }

    public static void logInvalidHost(InetAddress inetAddress) {
        LOGGER.warn("Invalid host: {}", inetAddress.getHostAddress());
    }

    public static void logInvalidHost(String host) {
        LOGGER.warn("Invalid host: {}", host);
    }

    public static void logInvalidPortNumbers(String... ports) {
        StringBuilder invalidPortsStr = new StringBuilder();
        for (int i = 0; i < ports.length; i++) {
            if (i == ports.length - 1) {
                invalidPortsStr.append(ports[i]);
            } else {
                invalidPortsStr.append(ports[i]).append(", ");
            }
        }
        LOGGER.warn("Invalid ports: {}", invalidPortsStr);
    }

    public static void logInvalidPortRange(String range) {
        LOGGER.warn("Invalid port range: {}", range);
    }
}
