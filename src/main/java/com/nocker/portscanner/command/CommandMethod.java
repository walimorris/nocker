package com.nocker.portscanner.command;

import java.lang.reflect.Method;

public class CommandMethod {
    private final String commandMethodName;
    private final String canonicalMethodName;
    private final Method method;

    public CommandMethod(String commandMethodName, String canonicalMethodName, Method method) {
        this.commandMethodName = commandMethodName;
        this.canonicalMethodName = canonicalMethodName;
        this.method = method;
    }

    public String getCommandMethodName() {
        return commandMethodName;
    }

    public String getCanonicalMethodName() {
        return canonicalMethodName;
    }

    public Method getMethod() {
        return method;
    }
}
