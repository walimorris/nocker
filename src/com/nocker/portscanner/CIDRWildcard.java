package com.nocker.portscanner;

import static com.nocker.portscanner.PortScannerUtil.*;

public class CIDRWildcard {
    private final String value;
    private String address;
    private final String cidr;
    private final Integer[] octets;

    private static final String ALLOWED_CIDR = "/24";

    public CIDRWildcard(String value) {
        this.value = value;
        if (!value.contains("/")) {
            throw new IllegalArgumentException("Missing CIDR suffix: " + value);
        }
        String[] parts = value.split("/");
        this.address = parts[0];
        this.cidr = "/" + parts[1];
        if (!ALLOWED_CIDR.equals(cidr)) {
            throw new IllegalArgumentException("Unsupported CIDR: " + cidr);
        }
        this.octets = convertToIntegerArray(address.split("\\."));
        if (!isValidIPOctets()) {
            throw new IllegalArgumentException("Invalid IP address: " + address);
        }
    }

    public String getValue() {
        return value;
    }

    public String getAddress() {
        return address;
    }

    public String getCidr() {
        return cidr;
    }

    public Integer[] getOctets() {
        return octets;
    }

    public boolean isValidCIDRWildcard() {
        return containsValidOctets();
    }

    public void incrementLastOctet() {
        if (octets[3] < 255) {
            ++octets[3];
            address = stringifyArray(octets);
        }
    }

    private boolean containsValidOctets() {
        if (!value.contains(".")) {
            return false;
        }
        return cidr.equals(ALLOWED_CIDR) && isValidIPOctets();
    }

    private boolean isValidIPOctets() {
        if (octets == null || octets.length != 4) {
            return false;
        }
        for (int octetValue : octets) {
            if (octetValue < 0 || octetValue > 255) {
                return false;
            }
        }
        return true;
    }
}
