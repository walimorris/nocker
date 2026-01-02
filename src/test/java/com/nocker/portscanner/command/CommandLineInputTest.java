package com.nocker.portscanner.command;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CommandLineInputTest {

    @Test
    void parseThrowsBecauseFlagNotPrependedWithSingleDash() {
        String[] args = new String[]{
                "nocker", "scan", "--host=localhost", "--port=8080", "timeout=100", "out=results.json"
        };
        Assertions.assertThrows(InvalidCommandException.class, () -> CommandLineInput.parse(args));
    }

    @Test
    void parseThrowsBecauseArgNotALegalArg() {
        String[] args = new String[]{
                "nocker", "scan", "-host=localhost", "--port=8080", "timeout=100", "out=results.json"
        };
        Exception exception = Assertions.assertThrows(InvalidCommandException.class, () -> CommandLineInput.parse(args));
        Assertions.assertTrue(exception.getMessage().contains("Illegal flag"));
        Assertions.assertTrue(exception.getMessage().contains("-host"));
    }

    @Test
    void parseThrowsBecauseOfNonSenseCommand() {
        String[] args = new String[]{
                "nocker",  "scan", "--hacktheworld=true", "--ports=ALL"
        };
        Exception exception = Assertions.assertThrows(InvalidCommandException.class, () -> CommandLineInput.parse(args));
        System.out.println(exception.getMessage());
        Assertions.assertEquals("This is a illegal argument[--hacktheworld]", exception.getMessage());
    }

    @Test
    void getCommand() {
    }

    @Test
    void getArguments() {
    }

    @Test
    void getCommandMethod() {

    }

    @Test
    void getFlagsAbbreviation() {
        String[] args = new String[]{"nocker", "scan", "--host=localhost", "--port=8080", "-t", "100", "-o", "results.json"};
        CommandLineInput commandLineInput = CommandLineInput.parse(args);
        Map<String, String> flags = commandLineInput.getFlags();
        Assertions.assertEquals(2, flags.size());
    }

    @Test
    void getFlagsAbbreviationReversedArguments() {
        String[] args = new String[]{"nocker", "scan", "-t", "100", "--port=8080", "--host=localhost", "-o", "results.json"};
        CommandLineInput commandLineInput = CommandLineInput.parse(args);
        Map<String, String> flags = commandLineInput.getFlags();
        Assertions.assertEquals(2, flags.size());
    }

    @Test
    void getFlagsFullname() {
        String[] args = new String[]{"nocker", "scan", "--host=localhost", "--port=8080", "-timeout", "100", "-out", "results.json"};
        CommandLineInput commandLineInput = CommandLineInput.parse(args);
        Map<String, String> flags = commandLineInput.getFlags();
        Assertions.assertEquals(2, flags.size());
    }

    @Test
    void getFlagsFullnameWithEquals() {
        String[] args = new String[]{"nocker", "scan", "--host=localhost", "--port=8080", "-timeout=100", "-out=results.json"};
        CommandLineInput commandLineInput = CommandLineInput.parse(args);
        Map<String, String> flags = commandLineInput.getFlags();
        Assertions.assertEquals(2, flags.size());
    }
}