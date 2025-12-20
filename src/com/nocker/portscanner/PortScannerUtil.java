package com.nocker.portscanner;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class PortScannerUtil {
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
            System.out.println("Host not found: " + host);
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
        System.out.println("invalid port: " + port);
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
        System.out.println("invalid ports: " + invalidPortsStr);
    }

    public static void logInvalidPortRange(String range) {
        System.out.println("invalid port range: " + range);
    }
}
