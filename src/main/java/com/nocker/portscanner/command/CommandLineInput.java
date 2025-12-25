package com.nocker.portscanner.command;

import com.nocker.portscanner.Flag;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

public class CommandLineInput {
    public String[] commandLineInput;
    public static Set<Flag> flags = Flag.flagValues();

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
    // fully valid arguments in nocker contains '='
    // example 'nocker scan --host=localhost --port=8080 -ax'
    // -ax is a special argument with enhancements on the
    // fully qualified commandLine input
    public Map<String, String> getArguments() {
        Map<String, String> arguments = new HashMap<>();
        List<String> shortenedInput = shortenInput();
        if (CollectionUtils.isNotEmpty(shortenedInput)) {
            for (String token : shortenedInput) {
                if (!fromFlagToken(token).isPresent()) {
                    addArgToken(token, arguments);
                }
            }
        }
        return arguments;
    }

     // works, but can be optimized by skipping the next token if
     // the previous token was an abbreviation ex: -t 5000
     public Map<String, String> getFlags() {
        Map<String, String> flagsHash = new HashMap<>();
        List<String> shortenedInput = shortenInput();

        if (CollectionUtils.isNotEmpty(shortenedInput)) {
            for (int i = 0; i < shortenedInput.size(); i++) {
                String token = shortenedInput.get(i);
                Optional<Flag> flag = fromFlagToken(token);
                if (flag.isPresent()) {
                    boolean addedWithArgNormalization = addArgToken(token, flagsHash);
                    if (!addedWithArgNormalization) {
                        addFlagToken(token, flag.get(), flagsHash, shortenedInput.get(i+1));
                    }
                }
            }
        }
        return flagsHash;
    }

    private String normalizeToken(String token) {
        token = token.trim();
        token = token.startsWith("--") ? token.substring(2) : token;
        token = token.contains("=") ? token.split("=")[0] : token;
        return token.trim();
    }

    private boolean addArgToken(String token, Map<String, String> hash) {
        if (token.startsWith("--") && token.contains("=")) {
            String argumentValue;
            String argument;
            String value;
            argumentValue = token.substring(2);
            argument = argumentValue.split("=")[0];
            value = argumentValue.split("=")[1];
            hash.put(argument, value);
            return true;
        }
        return false;
    }

    private void addFlagToken(String token, Flag flag, Map<String, String> hash, String nextToken) {
        if (token.startsWith("-")) {
            hash.put(flag.getFullName(), nextToken);
        }
    }

    private List<String> shortenInput() {
        List<String> input = Arrays.asList(this.commandLineInput);
        if (CollectionUtils.isNotEmpty(input)) {
            // ignore namespace and command
            return input.subList(2, input.size());
        }
        return null;
    }

    private Optional<Flag> fromFlagToken(String token) {
        String normalizedToken = normalizeToken(token);
        return Arrays.stream(Flag.values())
                .filter(f -> f.getAbbreviatedName().equals(normalizedToken) || f.getFullName().equals(normalizedToken))
                .findFirst();
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
