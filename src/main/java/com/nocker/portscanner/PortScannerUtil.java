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

    /**
     * Converts an array of {@code String} objects into an array of {@code Integer} objects.
     * Each string in the input array is parsed into an integer.
     *
     * @param objects an array of {@code String} objects to be converted to {@code Integer} objects
     * @return an array of {@code Integer} objects parsed from the input string array
     * @throws NumberFormatException if any string in the input array cannot be parsed as an integer
     */
    public static Integer[] convertToIntegerArray(String[] objects) {
        return Arrays.stream(objects)
                .map(Integer::parseInt)
                .toArray(Integer[]::new);
    }

    /**
     * Converts a list of {@code String} objects into a list of {@code Integer} objects.
     * Each string in the input list is parsed into an integer.
     *
     * @param strings a list of {@code String} objects to be converted to {@code Integer} objects
     * @return a list of {@code Integer} objects parsed from the input string list
     * @throws NumberFormatException if any string in the input list cannot be parsed as an integer
     */
    public static List<Integer> convertToIntegerList(List<String> strings) {
        return strings.stream()
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Converts an array of {@code Integer} objects into an array of {@code String} objects.
     * Each integer in the input array is transformed into its string representation.
     *
     * @param objects an array of {@code Integer} objects to be converted to {@code String} objects
     * @return an array of {@code String} objects representing the string equivalent of each integer in the input array
     */
    public static String[] convertToStringArray(Integer[] objects) {
        return Arrays.stream(objects)
                .map(String::valueOf)
                .toArray(String[]::new);
    }

    /**
     * Converts a list of {@code Integer} objects into a list of {@code String} objects.
     * Each integer in the input list is transformed into its string representation.
     *
     * @param integers a list of {@code Integer} objects to be converted to {@code String} objects
     * @return a list of {@code String} objects representing the string equivalents of each integer
     */
    public static List<String> convertToStringList(List<Integer> integers) {
        return integers.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Converts an array of {@code Integer} objects into a single {@code String},
     * where each integer is represented as its string equivalent and concatenated
     * with a period (".") delimiter.
     *
     * @param objects an array of {@code Integer} objects to be converted into
     *                a single {@code String} representation
     * @return a {@code String} formed by concatenating the string equivalents of
     *         the integers in the input array, separated by periods
     */
    public static String stringifyArray(Integer[] objects) {
        return String.join(".", convertToStringArray(objects));
    }

    /**
     * Converts an array of {@code String} objects into a single {@code String},
     * where each element is concatenated and separated by a period (".") delimiter.
     *
     * @param objects an array of {@code String} objects to be joined into a single string
     * @return a {@code String} that is the result of concatenating all elements of the input
     *         array, separated by periods
     */
    public static String stringifyArray(String[] objects) {
        return String.join(".", objects);
    }

    /**
     * Resolves the IP address for the given hostname. If the host is recognized as
     * the local machine, the local address is returned. Otherwise, it attempts to
     * resolve the hostname to its corresponding network address.
     *
     * @param host the name of the host to resolve; can be a hostname or IP address
     *             as a string. If the host is determined to be "localhost" or its
     *             equivalent, it resolves to the local address.
     * @return an {@code InetAddress} representing the resolved address of the
     *         specified host, or {@code null} if the hostname cannot be resolved.
     */
    public static InetAddress getHostAddress(String host) {
        InetAddress hostAddress = null;
        try {
            hostAddress = isLocalHost(host) ? InetAddress.getLocalHost() : InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            LOGGER.error("Fatality, unknown host [{}]: {}", host, e.getMessage(), e);
        }
        return hostAddress;
    }

    /**
     * Resolves the IPv4 address of the specified host. If the host is recognized
     * as the local machine, the local IPv4 address is returned. Otherwise, it
     * attempts to resolve the hostname or IP address to its corresponding IPv4
     * address.
     *
     * @param host the name of the host to resolve; can be a hostname or an
     *             IPv4 address as a string. If the host is determined to be
     *             "localhost" or its equivalent, it resolves to the local address.
     * @return an {@code Inet4Address} representing the resolved IPv4 address of
     *         the specified host, or {@code null} if the hostname cannot be resolved.
     */
    public static Inet4Address getHostInet4Address(String host) {
        Inet4Address hostAddress = null;
        try {
            hostAddress = isLocalHost(host) ? (Inet4Address) Inet4Address.getLocalHost() : (Inet4Address) Inet4Address.getByName(host);
        } catch (UnknownHostException e) {
            LOGGER.error("Fatality, unknown host [{}]: {}", host, e.getMessage(), e);
        }
        return hostAddress;
    }

    /**
     * Resolves the hostname for the specified IPv4 host. If the given host is
     * recognized as the local machine (e.g., "localhost"), this method returns
     * the local machine's hostname. Otherwise, it attempts to resolve the
     * hostname for the provided host address.
     *
     * @param host the name of the host to resolve; can be a hostname or an
     *             IPv4 address as a string. If the host is determined to
     *             be "localhost" or its equivalent, it returns the hostname
     *             of the local machine.
     * @return the hostname of the specified host as a string, or null if the
     *         hostname cannot be resolved.
     */
    public static String getHostInet4AddressName(String host) {
        String hostAddressName = null;
        try {
            hostAddressName = isLocalHost(host) ? Inet4Address.getLocalHost().getHostName() : Inet4Address.getByName(host).getHostName();
        } catch (UnknownHostException e) {
            LOGGER.error("Fatality, unknown host [{}]: {}", host, e.getMessage(), e);
        }
        return hostAddressName;
    }

    /**
     * Determines if the provided hostname corresponds to "localhost".
     * The method checks if the input string is non-blank and, if so,
     * evaluates whether it contains "localhost" (case-insensitive).
     *
     * @param host the hostname or IP address as a string to be checked.
     *             A null or blank string will return false.
     * @return {@code true} if the provided hostname corresponds to "localhost"
     *         or contains "localhost", {@code false} otherwise.
     */
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

    /**
     * Converts a port number provided as a {@code String} into an {@code int}.
     * If the input string cannot be parsed into an integer, it logs the invalid port
     * using {@code logInvalidPortNumber} and returns 0.
     *
     * @param p the port number as a {@code String} to be converted to an {@code int}
     * @return the parsed port number as an {@code int}, or 0 if the input is invalid
     */
    public static int converPortToInteger(String p) {
        try {
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            logInvalidPortNumber(p);
            return 0;
        }
    }

    /**
     * Checks if the given port number, represented as a {@code String}, is valid.
     * A valid port number must be within the range of acceptable port numbers defined by {@code MIN_PORT} and {@code MAX_PORT}.
     * This method converts the string representation of the port to an integer before validation.
     *
     * @param p the port number as a {@code String} to be validated
     * @return {@code true} if the port number is valid; {@code false} otherwise
     */
    public static boolean isValidPortNumber(String p) {
        return isValidPortNumber(converPortToInteger(p));
    }

    /**
     * Validates whether the given port number is within the valid range
     * of acceptable port numbers as defined by MIN_PORT and MAX_PORT.
     *
     * @param p the port number represented as an {@code int} to be validated
     * @return {@code true} if the port number is valid; {@code false} otherwise
     */
    public static boolean isValidPortNumber(int p) {
        return (p >= MIN_PORT && p <= MAX_PORT);
    }

    /**
     * Checks if all provided strings represent valid port numbers.
     *
     * A valid port number is typically an integer within the range
     * of 1 to 65535, represented as a string.
     *
     * @param ports an array of strings to be checked as valid port numbers
     * @return true if all strings represent valid port numbers, otherwise false
     */
    public static boolean allValidPortNumbers(String... ports) {
        for (String port : ports) {
            if (!isValidPortNumber(port)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates whether all items in the provided list of ports are valid port numbers.
     * A valid port number is typically an integer within the range of 1 to 65535.
     * The method supports lists containing integers or strings representing port numbers.
     *
     * @param ports the list of port objects to validate. It should contain either integers
     *              or strings. If the list is empty or contains unsupported types, an
     *              IllegalStateException will be thrown.
     * @return {@code true} if all items in the list are valid port numbers, otherwise {@code false}.
     * @throws IllegalStateException if the list is empty or contains unsupported types.
     */
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

    /**
     * Filters and returns a list of valid port numbers from the given list of port strings.
     *
     * @param ports the list of port numbers as strings to be validated
     * @return a list of valid port numbers as strings
     */
    public static List<String> filterInvalidPorts(List<String> ports) {
        List<String> validPorts = new ArrayList<>();
        for (String port : ports) {
            if (isValidPortNumber(port)) {
                validPorts.add(port);
            }
        }
        return validPorts;
    }

    /**
     * Converts a list of port numbers represented as strings into a list of integers.
     * Invalid port strings are filtered out before conversion.
     *
     * @param ports the list of port numbers as strings to be converted
     * @return a list of integers representing the valid port numbers
     */
    public static List<Integer> convertListOfPortStringsToIntegers(List<String> ports) {
        List<String> validPorts = filterInvalidPorts(ports);
        List<Integer> convertedPorts = new ArrayList<>();
        for (String port : validPorts) {
            convertedPorts.add(converPortToInteger(port));
        }
        return convertedPorts;
    }

    /**
     * Sorts a list of port strings numerically and returns the sorted list
     * as strings.
     *
     * @param ports the list of port strings to be sorted
     * @return a new list of sorted port strings
     */
    public static List<String> sortStringListPorts(List<String> ports) {
        List<Integer> integerPorts = sortIntegerListPorts(convertListOfPortStringsToIntegers(ports));
        return convertToStringList(integerPorts);
    }

    /**
     * Sorts a list of integers in ascending order.
     *
     * @param ports the list of integers to be sorted
     * @return a new list of integers sorted in ascending order
     */
    public static List<Integer> sortIntegerListPorts(List<Integer> ports) {
        return ports.stream().sorted().collect(Collectors.toList());
    }

    /**
     * Checks if all provided integers are valid port numbers.
     *
     * A valid port number is assumed to be within a specific range as defined
     * by the implementation of the isValidPortNumber method.
     *
     * @param ports an array of integers representing port numbers to validate
     * @return true if all integers in the provided array are valid port numbers,
     *         false if any of the integers fail validation
     */
    public static boolean allIsValidPortNumbers(int... ports) {
        for (int port : ports) {
            if (!isValidPortNumber(port)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines the port range from a given list of ports. The list can contain
     * either integers or strings representing port values. The method processes
     * the list to find the minimum and maximum ports, creating a PortRange object.
     *
     * @param ports a list containing port values, either as integers or strings.
     *              The list must not be empty and must contain only elements of
     *              the same data type (either all integers or all strings).
     * @return a PortRange object that represents the range of ports, with the
     *         minimum and maximum ports derived from the input list.
     * @throws IllegalStateException if the provided list is empty, or if the elements
     *                               are of unsupported types, or if they are of mixed types.
     */
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
