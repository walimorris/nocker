package com.nocker.command;

import com.nocker.portscanner.PortScanner;
import com.nocker.portscanner.command.CommandLineInput;
import com.nocker.portscanner.command.InvocationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Order:
 * 1. get all methods with command
 * 2. get all methods with param count
 * 3. get all methods with exact params
 * 4. maybe we should use something like @primary annotation for tiebreakers
 */
public final class CommandEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandEngine.class);

    private CommandEngine() {
        throw new AssertionError("Cannot instantiate instance of final class" + getClass().getName());
    }

    // retrieve the method
    public static InvocationCommand retrieve(CommandLineInput commandLineInput) {
        String command = commandLineInput.getCommand();
        LinkedHashMap<String, String> arguments = commandLineInput.getArguments();

        // filter 1: filter methods with command - will need to update this and pull class from command invocation
        List<Method> methodsFromCommand = MethodResolver.filterMethodsFromCommand(command, PortScanner.class);
        // filter 2: filter from param count
        List<Method> methodsFromParamCount = MethodResolver.filterMethodsByParameterCount(methodsFromCommand, arguments.size());
        // filter 3: filter by exact param
        return retrieveInvocationCommandFromMethodExtraction(commandLineInput, methodsFromParamCount, arguments);
    }

    private static InvocationCommand retrieveInvocationCommandFromMethodExtraction(CommandLineInput commandLineInput, List<Method> methods,
                                                               LinkedHashMap<String, String> arguments) {
        Method winningMethod = null;
        LinkedHashMap<String, Class> parameters = null;
        LinkedHashMap<String, Class> args = ArgumentConverter.getArgumentNamesAndTypes(arguments);
        for (Method method : methods) {
            LinkedHashMap<String, Class> currentParameters = MethodResolver.getParameterNamesAndTypes(method);
            if (currentParameters.equals(args)) {
                winningMethod = method;
                parameters = currentParameters;
                break;
            }
        }
        if (winningMethod == null) {
            return null; // should throw here or output some useful info
        }
        Object[] commandArgs = ArgumentConverter.convertToObjectArray(parameters, arguments);
        return new InvocationCommand(commandLineInput, winningMethod, commandArgs);
    }
}
