package com.nocker.portscanner.tasks;

public class PortRange {
    private final int low;
    private final int high;

    public PortRange(int low, int high) {
        this.low = low;
        this.high = high;
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }

    @Override
    public String toString() {
        return "[" + low + " - " + high + "]";
    }
}
