package com.nocker.command;

import com.nocker.portscanner.PortScanner;
import com.nocker.portscanner.command.CommandLineInput;
import com.nocker.portscanner.command.CommandMethod;
import com.nocker.portscanner.command.InvocationRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class CommandEngineTest {

    @Test
    void retrieve() throws NoSuchMethodException {
        String command = "nocker scan --host=127.0.0.1 --port=8080";
        String basicScanMethodName = "scanSingleHostAndSinglePort";
        Method basicScanMethod = PortScanner.class.getDeclaredMethod(basicScanMethodName, String.class, int.class);
        CommandMethod commandMethod = new CommandMethod("scan", basicScanMethodName, basicScanMethod);
        Object[] basicScanObjectArgs = new Object[]{"127.0.0.1", "8080"};
        LinkedHashMap<String, String> basicScanArgs = new LinkedHashMap<String, String>() {{
            put("host", "127.0.0.1");
            put("port", "8080");
        }};
        CommandLineInput commandLineInput = new CommandLineInput(command, commandMethod, basicScanArgs, null);
        InvocationRequest expectedInvocationRequest = new InvocationRequest(commandLineInput, basicScanMethod, basicScanObjectArgs);
        InvocationRequest actualInvocationRequest = CommandEngine.retrieve(commandLineInput);

        assertEquals(expectedInvocationRequest.getCommandLineInput(), actualInvocationRequest.getCommandLineInput());
        assertEquals(expectedInvocationRequest.getMethod(), actualInvocationRequest.getMethod());
        assertEquals(2, actualInvocationRequest.getArgs().length);
        assertEquals(expectedInvocationRequest.getArgs()[0], actualInvocationRequest.getArgs()[0]);
        assertEquals(expectedInvocationRequest.getArgs()[1], String.valueOf(actualInvocationRequest.getArgs()[1]));
    }

    @Test
    void retrieveThrowsIllegalStateExceptionWithNullArguments() throws NoSuchMethodException {
        String command = "nocker scan --host=127.0.0.1 --port=8080";
        String basicScanMethodName = "scanSingleHostAndSinglePort";
        Method basicScanMethod = PortScanner.class.getDeclaredMethod(basicScanMethodName, String.class, int.class);
        CommandMethod commandMethod = new CommandMethod("scan", basicScanMethodName, basicScanMethod);
        CommandLineInput commandLineInput = new CommandLineInput(command, commandMethod, null, null);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            CommandEngine.retrieve(commandLineInput);
        });
        assertEquals("CommandLineInput cannot be null.", exception.getMessage());
    }
}