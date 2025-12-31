package com.nocker;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum Arg {
    HOST("host"),
    HOSTS("hosts"),
    PORT("port"),
    PORTS("ports");

    private final String argumentName;

    private Arg(String argumentName) {
        this.argumentName = argumentName;
    }

    public String getArgumentName() {
        return this.argumentName;
    }

    public static Set<Arg> argValues() {
        return EnumSet.allOf(Arg.class);
    }

    public static Set<String> argStringValues() {
        return argValues().stream()
                .map(Arg::getArgumentName)
                .collect(Collectors.toSet());
    }
}
