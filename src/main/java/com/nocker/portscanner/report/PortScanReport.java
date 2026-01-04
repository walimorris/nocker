package com.nocker.portscanner.report;

import com.nocker.portscanner.scheduler.PortScanScheduler;

import java.util.List;

public class PortScanReport {
    private final PortScanScheduler portScanScheduler;
    private final List<PortScanResult> results;
    private final ScanSummary summary;

    public PortScanReport(PortScanScheduler scheduler, List<PortScanResult> results, ScanSummary summary) {
        this.portScanScheduler = scheduler;
        this.results = results;
        this.summary = summary;
    }

    public PortScanScheduler getPortScanScheduler() {
        return this.portScanScheduler;
    }

    public List<PortScanResult> getResults() {
        return this.results;
    }

    public ScanSummary getSummary() {
        return this.summary;
    }
}
