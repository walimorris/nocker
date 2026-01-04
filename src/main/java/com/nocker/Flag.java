package com.nocker;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum Flag {
    TIMEOUT("timeout", "t"),
    CONCURRENCY("concurrency", "c"),
    SYN("sneak", "s"),
    FORMAT("format", "f"),
    OUT("out", "o"),
    ROBUST("robust", "r");

    private final String full;
    private final String abbr;

    private Flag(String full, String abbr) {
        this.full = full;
        this.abbr = abbr;
    }

    public String getFullName() {
        return this.full;
    }

    public String getAbbreviatedName() {
        return this.abbr;
    }

    public static Set<Flag> flagValues() {
        return EnumSet.allOf(Flag.class);
    }

    public static Set<String> flagStringValues() {
        return flagValues().stream()
                .map(Flag::getFullName)
                .collect(Collectors.toSet());
    }
}
