package com.nocker.portscanner.command;

import java.lang.reflect.Method;

public class InvocationCommand {
    private final CommandLineInput commandLineInput;
    private final Method method;
    private final Object[] args;

    public InvocationCommand(CommandLineInput commandLineInput, Method method, Object[] args) {
        this.commandLineInput = commandLineInput;
        this.method = method;
        this.args = args;
    }

    public CommandLineInput getCommandLineInput() {
        return commandLineInput;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return "InvocationCommand{" +
                "commandLineInput=" + commandLineInput +
                ", method=" + method +
                ", args=" + (args != null ? java.util.Arrays.toString(args) : "null") +
                '}';
    }
}
