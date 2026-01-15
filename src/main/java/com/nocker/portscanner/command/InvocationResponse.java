package com.nocker.portscanner.command;

import com.nocker.portscanner.PortScanner;

import java.lang.reflect.InvocationTargetException;

public class InvocationResponse {

    private InvocationResponse() {}

    public static String invoke(InvocationRequest invocationRequest, PortScanner portScanner) throws InvocationTargetException, IllegalAccessException {
        if (invocationRequest == null || portScanner == null) {
            throw new IllegalArgumentException("Must supply valid request and port scanner");
        }
        return (String) invocationRequest.getMethod().invoke(portScanner, invocationRequest.getArgs());
    }
}
