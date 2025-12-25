package com.nocker.portscanner.wildcard;

import com.nocker.portscanner.PortScannerUtil;

public class PortWildcard {
    private final String value;
    private final int lowPort;
    private final int highPort;

    public PortWildcard(String value) {
        this.value = value;
        if (!value.contains("-")) {
            throw new IllegalArgumentException("missing - between port ranges");
        }
        String[] parts = value.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("invalid port range");
        }
        this.lowPort = PortScannerUtil.converPortToInteger(parts[0]);
        this.highPort = PortScannerUtil.converPortToInteger(parts[1]);
        if (lowPort > highPort) {
            throw new IllegalArgumentException("ports must be in a valid low to high range ex:[8080-8180]");
        }
        if (!PortScannerUtil.allValidPortNumbers(parts)) {
            throw new IllegalArgumentException("ensure ports are valid port ranges");
        }
    }

    public String getValue() {
        return value;
    }

    public int getLowPort() {
        return lowPort;
    }

    public int getHighPort() {
        return highPort;
    }
}
