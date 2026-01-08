package com.nocker.portscanner.model;

import java.net.Inet4Address;

/**
 * Represents the identity details of a host, including its IPv4 address,
 * host address, and hostname. This class is immutable and provides
 * access to the host's identity attributes via getter methods.
 * It is primarily constructed using the static {@link Builder} class.
 */
public class HostIdentity {
    private final Inet4Address hostInet4Address;
    private final String hostAddress;
    private final String hostname;

    public HostIdentity(Inet4Address hostInet4Address, String hostAddress, String hostname) {
        this.hostInet4Address = hostInet4Address;
        this.hostAddress = hostAddress;
        this.hostname = hostname;
    }

    HostIdentity(Builder builder) {
        this.hostInet4Address = builder.hostInet4Address;
        this.hostAddress = builder.hostAddress;
        this.hostname = builder.hostname;
    }

    /**
     * Retrieves the IPv4 address associated with the host.
     *
     * @return the {@code Inet4Address} representing the IPv4
     * address of the host
     */
    public Inet4Address getHostInet4Address() { return hostInet4Address; }

    /**
     * Retrieves the host address as a string.
     *
     * @return the host address associated with this host
     * identity
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * Retrieves the hostname of the host associated with
     * this identity.
     *
     * @return the hostname as a string
     */
    public String getHostname() {
        return hostname;
    }

    public static class Builder {
        private Inet4Address hostInet4Address;
        private String hostAddress;
        private String hostname;

        public Builder hostInet4Address(Inet4Address hostInet4Address) {
            this.hostInet4Address = hostInet4Address;
            return this;
        }

        public Builder hostAddress(String hostAddress) {
            this.hostAddress = hostAddress;
            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public HostIdentity build() {
            return new HostIdentity(this);
        }
    }
}
