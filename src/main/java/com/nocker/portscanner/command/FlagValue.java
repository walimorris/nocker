package com.nocker.portscanner.command;

import com.nocker.Flag;

public class FlagValue {
    private final Flag flag;
    private final String value;

    public FlagValue(Flag flag, String value) {
        this.flag = flag;
        this.value = value;
    }

    public Flag getFlag() {
        return this.flag;
    }

    public String getValue() {
        return this.value;
    }
}
