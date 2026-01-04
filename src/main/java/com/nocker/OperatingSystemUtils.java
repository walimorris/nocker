package com.nocker;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperatingSystemUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperatingSystemUtils.class);

    /**
     * The following properties are intended to be used with
     * {@link System#getProperty(String)}
     */
    private static final String OS_NAME = "os.name";
    private static final String USER_NAME = "user.name";

    private OperatingSystemUtils() {}

    public static String getOperatingSystem() {
        String os = System.getProperty(OS_NAME);
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

    public static String currentUser() {
        String user = "anonymous";
        try {
            user = System.getProperty(USER_NAME);
        } catch (SecurityException e) {
            LOGGER.warn("Security Manager restricting access to sys props", e);
        }
        return user;
    }
}
