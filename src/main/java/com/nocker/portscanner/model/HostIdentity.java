package com.nocker.portscanner.model;

public class HostIdentity {
    private String hostAddress;
    private String hostname;

    HostIdentity(Builder builder) {
        this.hostAddress = builder.hostAddress;
        this.hostname = builder.hostname;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public static class Builder {
        private String hostAddress;
        private String hostname;

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
