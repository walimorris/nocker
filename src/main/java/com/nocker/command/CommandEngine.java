package com.nocker.command;

import com.nocker.portscanner.command.CommandLineInput;
import com.nocker.portscanner.command.InvocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

// TODO: The name of this class is misleading - it's no dang engine! It's barely doing anything.
public final class CommandEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandEngine.class);

    private CommandEngine() {
        throw new AssertionError("Cannot instantiate instance of final class" + getClass().getName());
    }

    // retrieve the method
    public static InvocationRequest retrieve(CommandLineInput cl) {
        if (cl.getCommand() == null || cl.getCommandMethod() == null || cl.getArguments() == null) {
            throw new IllegalStateException("CommandLineInput cannot be null.");
        }
        LinkedHashMap<String, Class> currentParameters = MethodResolver
                .getNockerParameterNamesAndTypes(cl.getCommandMethod().getMethod());

        Object[] commandArgs = ArgumentConverter.convertToObjectArray(currentParameters, cl.getArguments());
        return new InvocationRequest(cl, cl.getCommandMethod().getMethod(), commandArgs);
    }
}
