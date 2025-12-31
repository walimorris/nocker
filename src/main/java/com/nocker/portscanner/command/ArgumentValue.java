package com.nocker.portscanner.command;

import com.nocker.Arg;

public class ArgumentValue {
    private final Arg argument;
    private final String value;

    public ArgumentValue(Arg argument, String value) {
        this.argument = argument;
        this.value = value;
    }

    public Arg getArgument() {
        return this.argument;
    }

    public String getValue() {
        return this.value;
    }
}
