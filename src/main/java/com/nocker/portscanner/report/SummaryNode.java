package com.nocker.portscanner.report;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SummaryNode {
    private String summaryFor;
    private String invocationCommand;
    private long duration;
    private UUID schedulerId;
    private int totalPortsScanned;
    private int openPortsCount;
    private int closedPortsCount;
    private int filteredPortsCount;
    private Map<String, Set<Integer>> openPortsByHost;

    public SummaryNode() {}

    public SummaryNode(String summaryFor,
                       String invocationCommand,
                       long duration,
                       UUID schedulerId,
                       int totalPortsScanned,
                       int openPortsCount,
                       int closedPortsCount,
                       int filteredPortsCount,
                       Map<String, Set<Integer>> openPortsByHost) {
        this.summaryFor = summaryFor;
        this.invocationCommand = invocationCommand;
        this.duration = duration;
        this.schedulerId = schedulerId;
        this.totalPortsScanned = totalPortsScanned;
        this.openPortsCount = openPortsCount;
        this.closedPortsCount = closedPortsCount;
        this.filteredPortsCount = filteredPortsCount;
        this.openPortsByHost = openPortsByHost;
    }

    public String getSummaryFor() {
        return summaryFor;
    }

    public void setSummaryFor(String summaryFor) {
        this.summaryFor = summaryFor;
    }

    public String getInvocationCommand() {
        return invocationCommand;
    }

    public void setInvocationCommand(String invocationCommand) {
        this.invocationCommand = invocationCommand;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public UUID getSchedulerId() {
        return schedulerId;
    }

    public void setSchedulerId(UUID schedulerId) {
        this.schedulerId = schedulerId;
    }

    public int getTotalPortsScanned() {
        return totalPortsScanned;
    }

    public void setTotalPortsScanned(int totalPortsScanned) {
        this.totalPortsScanned = totalPortsScanned;
    }

    public int getOpenPortsCount() {
        return openPortsCount;
    }

    public void setOpenPortsCount(int openPortsCount) {
        this.openPortsCount = openPortsCount;
    }

    public int getClosedPortsCount() {
        return closedPortsCount;
    }

    public void setClosedPortsCount(int closedPortsCount) {
        this.closedPortsCount = closedPortsCount;
    }

    public int getFilteredPortsCount() {
        return filteredPortsCount;
    }

    public void setFilteredPortsCount(int filteredPortsCount) {
        this.filteredPortsCount = filteredPortsCount;
    }

    public Map<String, Set<Integer>> getOpenPortsByHost() {
        return openPortsByHost;
    }

    public void setOpenPortsByHost(Map<String, Set<Integer>> openPortsByHost) {
        this.openPortsByHost = openPortsByHost;
    }
}
