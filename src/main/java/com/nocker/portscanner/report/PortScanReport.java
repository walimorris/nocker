package com.nocker.portscanner.report;

import java.util.List;

public class PortScanReport {
    private final List<PortScanResult> results;
    private final ScanSummary summary;

    public PortScanReport(List<PortScanResult> results, ScanSummary summary) {
        this.results = results;
        this.summary = summary;
    }

    public List<PortScanResult> getResults() {
        return this.results;
    }

    public ScanSummary getSummary() {
        return this.summary;
    }
}
