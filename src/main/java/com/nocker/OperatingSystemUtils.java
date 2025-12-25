package com.nocker;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperatingSystemUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperatingSystemUtils.class);

    private OperatingSystemUtils() {}

    public static String getOperatingSystem() {
        String os = System.getProperty("os.name");
        LOGGER.info("Current OS: {}", os);
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
