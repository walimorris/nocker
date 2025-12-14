package com.nocker;

import java.util.EnumSet;
import java.util.Set;

public enum Flag {
    TIMEOUT("timeout", "-t"),
    CONCURRENCY("concurrency", "-c");

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
}
