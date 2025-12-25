package com.nocker;

import org.apache.commons.lang3.StringUtils;

public class OperatingSystemUtils {

    private OperatingSystemUtils() {}

    public static String getOperatingSystem() {
        String os = System.getProperty("os.name");
        System.out.println(os);
        return os;
    }

    public static boolean isMacOs() {
        String os = getOperatingSystem();
        if (StringUtils.isNotBlank(os)) {
            return os.toLowerCase().contains("mac") ||
                    os.toLowerCase().contains("unix");
        }
        return false;
    }

    public static boolean isWindows() {
        String os = getOperatingSystem();
        if (StringUtils.isNotBlank(os)) {
            return os.toLowerCase().contains("windows");
        }
        return false;
    }

    public static boolean isNix() {
        String os = getOperatingSystem();
        if (StringUtils.isNotBlank(os)) {
            return os.toLowerCase().contains("linux");
        }
        return false;
    }
}
