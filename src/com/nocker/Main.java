package com.nocker;

import com.nocker.annotations.AnnotationRetriever;
import com.nocker.commands.CommandLineInput;

import java.lang.reflect.Method;

public class Main {
    public static void main(String[] args) {
        String test = "nocker scan --host=localhost --port=8080 --aX";
        String[] args1 = test.split(" ");
        CommandLineInput commandLineInput = new CommandLineInput(args1);
        Method method = AnnotationRetriever.retrieve(commandLineInput);
        System.out.println(method.getName());
    }
}