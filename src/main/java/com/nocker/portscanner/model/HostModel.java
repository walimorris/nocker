package com.nocker.portscanner.model;

import com.nocker.portscanner.PortScanResult;

import java.util.List;
import java.util.UUID;

public class HostModel {
    private final UUID schedulerId;
    private final long durationMillis;
    private final HostIdentity hostIdentity;
    private final List<PortScanResult> tasks;

    HostModel(Builder builder) {
        this.schedulerId = builder.schedulerId;
        this.durationMillis = builder.durationMillis;
        this.hostIdentity = builder.hostIdentity;
        this.tasks = builder.tasks;
    }

    public UUID getSchedulerId() {
        return schedulerId;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public HostIdentity getHostIdentity() {
        return hostIdentity;
    }

    public List<PortScanResult> getTasks() {
        return tasks;
    }

    public static class Builder {
        private UUID schedulerId;
        private long durationMillis;
        private HostIdentity hostIdentity;
        private List<PortScanResult> tasks;

        public Builder schedulerId(UUID schedulerId) {
            this.schedulerId = schedulerId;
            return this;
        }

        public Builder durationMillis(long durationMillis) {
            this.durationMillis = durationMillis;
            return this;
        }

        public Builder hostIdentity(HostIdentity hostIdentity) {
            this.hostIdentity = hostIdentity;
            return this;
        }

        public Builder tasks(List<PortScanResult> tasks) {
            this.tasks = tasks;
            return this;
        }

        public HostModel build() {
            return new HostModel(this);
        }
    }
}
