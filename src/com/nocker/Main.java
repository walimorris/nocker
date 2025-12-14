package com.nocker;

import com.nocker.annotations.AnnotationRetriever;
import com.nocker.commands.CommandLineInput;
import com.nocker.commands.CommandService;
import com.nocker.commands.InvocationCommand;

import java.lang.reflect.InvocationTargetException;

public class Main {
    public static void main(String[] args) {
        String test = "nocker scan --host=scanme.nmap.org -t 5000 -c 200";
//        String test = "nocker scan --host=localhost -t 1000 --concurrency=155";
        String[] args1 = test.split(" ");
        CommandLineInput commandLineInput = new CommandLineInput(args1);
        InvocationCommand invocationCommand = AnnotationRetriever.retrieve(commandLineInput);
        invokeCommand(invocationCommand);
    }

    protected static void invokeCommand(InvocationCommand invocationCommand) {
        CommandService commandService = new CommandService(invocationCommand);
        try {
            invocationCommand.method().invoke(commandService, invocationCommand.args());
        } catch (InvocationTargetException | IllegalAccessException exception) {
            System.out.println(exception.getMessage());
        }
    }
}