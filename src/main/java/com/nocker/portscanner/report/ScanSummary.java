package com.nocker.portscanner.report;

import com.nocker.portscanner.PortState;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nocker.portscanner.PortState.*;

public class ScanSummary {
    private final AtomicInteger openPortsCount = new AtomicInteger();
    private final AtomicInteger filteredPortsCount = new AtomicInteger();
    private final AtomicInteger closedPortsCount = new AtomicInteger();
    private final Queue<Integer> openPorts = new ConcurrentLinkedQueue<>();
    private final long startTime;
    private long stopTime;

    public ScanSummary(long startNanos) {
        this.startTime = startNanos;
    }

    public void update(PortScanResult result) {
        PortState state = result.getState();
        if (state.equals(OPEN)) {
            openPortsCount.incrementAndGet();
            openPorts.add(result.getPort());
        } else if (state.equals(FILTERED)) {
            filteredPortsCount.incrementAndGet();
        } else {
            if (state.equals(CLOSED)) {
                closedPortsCount.incrementAndGet();
            }
        }
    }

    public long durationMillis() {
        return TimeUnit.MILLISECONDS.toMillis(stopTime - startTime);
    }

    public void stop() {
        this.stopTime = System.nanoTime();
    }

    public Queue<Integer> getOpenPorts() {
        return openPorts;
    }

    public int getOpenPortsCount() {
        return this.openPortsCount.get();
    }

    public int getFilteredPortsCount() {
        return this.filteredPortsCount.get();
    }

    public int getClosedPortsCount() {
        return this.closedPortsCount.get();
    }
}
