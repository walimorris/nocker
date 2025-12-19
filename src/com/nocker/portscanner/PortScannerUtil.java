package com.nocker.portscanner;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class PortScannerUtil {

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
}
