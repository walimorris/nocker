package com.nocker.portscanner;

import com.nocker.portscanner.tasks.PortRange;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public final class PortScannerUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortScannerUtil.class);

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private PortScannerUtil() {
        throw new AssertionError("PortScanUtil cannot be instantiated");
    }

    public static Integer[] convertToIntegerArray(String[] objects) {
        return Arrays.stream(objects)
                .map(Integer::parseInt)
                .toArray(Integer[]::new);
    }

    public static List<Integer> convertToIntegerList(List<String> strings) {
        return strings.stream()
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }

    public static String[] convertToStringArray(Integer[] objects) {
        return Arrays.stream(objects)
                .map(String::valueOf)
                .toArray(String[]::new);
    }

    public static List<String> convertToStringList(List<Integer> integers) {
        return integers.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
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

    public static String getHostInet4AddressName(String host) {
        String hostAddressName = null;
        try {
            hostAddressName = isLocalHost(host) ? Inet4Address.getLocalHost().getHostName() : Inet4Address.getByName(host).getHostName();
        } catch (UnknownHostException e) {
            LOGGER.error("Fatality, unknown host [{}]: {}", host, e.getMessage(), e);
        }
        return hostAddressName;
    }

    private static boolean isLocalHost(String host) {
        if (StringUtils.isNotBlank(host)) {
            host = host.toLowerCase();
            return host.contains("localhost");
        }
        return false;
    }

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

    public static boolean allValidPortNumbers(List<?> ports) {
        if (ObjectUtils.isEmpty(ports)) {
            throw new IllegalStateException("Cannot infer port data type from empty list");
        }
        Object obj = ports.get(0);
        if (!(obj instanceof Integer) && !(obj instanceof String)) {
            throw new IllegalStateException("Port range Lists must be string or integer types");
        }
        List<Integer> portsList = (List<Integer>) ports;
        for (int p : portsList) {
            if (!isValidPortNumber(p)) {
                return false;
            }
        }
        return true;
    }

    public static List<String> filterInvalidPorts(List<String> ports) {
        List<String> validPorts = new ArrayList<>();
        for (String port : ports) {
            if (isValidPortNumber(port)) {
                validPorts.add(port);
            }
        }
        return validPorts;
    }

    public static List<Integer> convertListOfPortStringsToIntegers(List<String> ports) {
        List<String> validPorts = filterInvalidPorts(ports);
        List<Integer> convertedPorts = new ArrayList<>();
        for (String port : validPorts) {
            convertedPorts.add(converPortToInteger(port));
        }
        return convertedPorts;
    }

    public static List<String> sortStringListPorts(List<String> ports) {
        List<Integer> integerPorts = sortIntegerListPorts(convertListOfPortStringsToIntegers(ports));
        return convertToStringList(integerPorts);
    }

    public static List<Integer> sortIntegerListPorts(List<Integer> ports) {
        return ports.stream().sorted().collect(Collectors.toList());
    }

    public static boolean allIsValidPortNumbers(int... ports) {
        for (int port : ports) {
            if (!isValidPortNumber(port)) {
                return false;
            }
        }
        return true;
    }

    public static PortRange getPortRange(List<?> ports) {
        if (ObjectUtils.isEmpty(ports)) {
            throw new IllegalStateException("Cannot infer port data type from empty list");
        }
        Object obj = ports.get(0);
        if (!(obj instanceof Integer) && !(obj instanceof String)) {
            throw new IllegalStateException("Port range Lists must be string or integer types");
        }
        if (obj instanceof String) {
            List<String> sortedStringPorts = sortStringListPorts((List<String>) ports);
            return new PortRange(converPortToInteger(sortedStringPorts.get(0)),
                    converPortToInteger(sortedStringPorts.get(sortedStringPorts.size() - 1)));
        }
        List<Integer> sortedIntPorts = sortIntegerListPorts((List<Integer>) ports);
        return new PortRange(sortedIntPorts.get(0), sortedIntPorts.get(sortedIntPorts.size() - 1));
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
