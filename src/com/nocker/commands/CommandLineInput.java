package com.nocker.commands;

import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

public class CommandLineInput {
    public String[] commandLineInput;

    public CommandLineInput(String[] commandLineInput) {
        this.commandLineInput = commandLineInput;
    }

    public String[] getCommandLineInput() {
        return this.commandLineInput;
    }

    public void setCommandLineInput(String[] commandLineInput) {
        this.commandLineInput = commandLineInput;
    }

    public String getCommand() {
        if (CollectionUtils.isEmpty(Arrays.asList(this.commandLineInput))) {
            throw new IllegalArgumentException("invalid command");
        }
        return this.commandLineInput[1].toLowerCase();
    }

    // check arguments are not null
    public Map<String, String> getArguments() {
        Map<String, String> arguments = new HashMap<>();
        List<String> input = Arrays.asList(this.commandLineInput);
        if (CollectionUtils.isNotEmpty(input)) {
            // ignore namespace and command
            List<String> shortenedInput = input.subList(2, input.size());
            for (String arg : shortenedInput) {
                // fully valid arguments in nocker contains '='
                // example 'nocker scan --host=localhost --port=8080 -ax'
                // -ax is a special argument with enhancements on the
                // fully qualified commandLine input
                if (arg.startsWith("--") && arg.contains("=")) {
                    String argumentValue;
                    String argument;
                    String value;
                    argumentValue = arg.substring(2);
                    argument = argumentValue.split("=")[0];
                    value = argumentValue.split("=")[1];
                    arguments.put(argument, value);
                }
            }
        }
        return arguments;
    }

    public String toString() {
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 0; i < this.commandLineInput.length; i++) {
            commandBuilder.append(this.commandLineInput[i]);
            if (!(i == commandLineInput.length - 1)) {
                commandBuilder.append(" ");
            }
        }
        return commandBuilder.toString();
    }
}
