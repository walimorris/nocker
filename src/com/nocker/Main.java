package com.nocker;

import com.nocker.annotations.AnnotationRetriever;
import com.nocker.portscanner.PortScanner;

import java.lang.reflect.InvocationTargetException;

public class Main {
    public static void main(String[] args) {
        String test = "nocker scan --host=scanme.nmap.org -t 5000 -c 200";
//        String test = "nocker scan --host=localhost --port=8080 -t 1000";
//        String test = "nocker scan --host=localhost -t 1000 --concurrency=155";
        // below command will default to 5000 ms timeout because it's below the low bound for timeouts (1000ms)
//        String test = "nocker scan --hosts=localhost,scanme.nmap.org --port=8080 -t 500 --concurrency=125";
//        String test = "nocker scan --hosts=localhost,scanme.nmap.org -t 1000 -c 200";
//        String test = "nocker cidr-scan --hosts=192.168.1.253/24 -t 1000 -c 300";
//        String test = "nocker scan --host=localhost --ports=0,1,8080,8081,8082,8083,8084";
//        String test = "nocker scan --host=localhost --ports=8080-8180 -t 1000";
        String[] args1 = test.split(" ");
        CommandLineInput commandLineInput = new CommandLineInput(args1);
        InvocationCommand invocationCommand = AnnotationRetriever.retrieve(commandLineInput);
        invokeCommand(invocationCommand);
    }

    protected static void invokeCommand(InvocationCommand invocationCommand) {
        PortScanner portScanner = new PortScanner(invocationCommand);
        try {
            invocationCommand.method().invoke(portScanner, invocationCommand.args());
        } catch (InvocationTargetException | IllegalAccessException exception) {
            System.out.println(exception.getMessage());
        }
    }
}