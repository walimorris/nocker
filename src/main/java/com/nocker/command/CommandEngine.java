package com.nocker.command;

import com.nocker.portscanner.command.CommandLineInput;
import com.nocker.portscanner.command.InvocationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class CommandEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandEngine.class);

    private CommandEngine() {
        throw new AssertionError("Cannot instantiate instance of final class" + getClass().getName());
    }

    // retrieve the method
    public static InvocationCommand retrieve(CommandLineInput cl) {
        return retrieveInvocationCommandFromMethodExtraction(cl);
    }

    private static InvocationCommand retrieveInvocationCommandFromMethodExtraction(CommandLineInput cl) {
        if (cl.getCommand() == null) {
            return null; // should throw here or output some useful info
        }
        LinkedHashMap<String, Class> currentParameters = MethodResolver
                .getNockerParameterNamesAndTypes(cl.getCommandMethod().getMethod());

        Object[] commandArgs = ArgumentConverter.convertToObjectArray(currentParameters, cl.getArguments());
        return new InvocationCommand(cl, cl.getCommandMethod().getMethod(), commandArgs);
    }
}
