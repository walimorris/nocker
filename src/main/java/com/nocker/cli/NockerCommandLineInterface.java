package com.nocker.cli;

import com.nocker.Flag;
import com.nocker.OperatingSystemUtils;
import com.nocker.command.CommandEngine;
import com.nocker.cli.formatter.HumanReadableFormatter;
import com.nocker.cli.formatter.JsonFormatter;
import com.nocker.cli.formatter.OutputFormatter;
import com.nocker.portscanner.PortScanner;
import com.nocker.portscanner.command.CommandLineInput;
import com.nocker.portscanner.command.InvocationRequest;
import com.nocker.portscanner.command.InvocationResponse;
import com.nocker.portscanner.scheduler.PortScanSynAckSchedulerFactory;
import com.nocker.writer.NockerFileWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;

import static com.nocker.Flag.*;
import static com.nocker.portscanner.PortScanner.*;

public final class NockerCommandLineInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(NockerCommandLineInterface.class);

    public static int run(String[] args) {
        if (OperatingSystemUtils.isMacOs()) {
            // TODO: add a step here to clean up lexical normalization (mainly extra spaces).
            // TODO: Sort of missed the boat on other lexical normalizations such as flag formats
            // but that can be refactored from (.parse()) at a later time.
            CommandLineInput commandLineInput = CommandLineInput.parse(args);
            InvocationRequest invocationRequest = Objects.requireNonNull(CommandEngine.retrieve(commandLineInput),
                    "Invocation Command is in legal null state.");
            String outPath = invocationRequest.getCommandLineInput()
                    .getFlags()
                    .getOrDefault(Flag.OUT.getFullName(), null);
            try (NockerFileWriter nockerFileWriter = outPath != null ? new NockerFileWriter(outPath) : null) {
                invokeCommand(invocationRequest, nockerFileWriter);
            } catch (IOException e) {
                LOGGER.error("Failed to write output: {}", e.getMessage(), e);
                return 2;
            }
        }  else {
            String os = OperatingSystemUtils.getOperatingSystem();
            LOGGER.warn(Marker.ANY_MARKER, "{} is not currently supported", os);
            return 1;
        }
        return 0;
    }

    private static void invokeCommand(InvocationRequest invocationRequest, NockerFileWriter nockerFileWriter) {
        Map<String, String> flags = invocationRequest.getCommandLineInput().getFlags();
        int concurrency = initConcurrency(flags);
        int timeout = initTimeout(flags);
        boolean syn = initSneakyLink(flags);
        boolean robust = initRobust(flags);
        OutputFormatter outputFormatter = initOutputFormatter(flags);
        PortScannerContext cxt = new PortScannerContext.Builder()
                .invocationCommand(invocationRequest).nockerFileWriter(nockerFileWriter)
                .schedulerFactory(new PortScanSynAckSchedulerFactory(invocationRequest,  concurrency))
                .outputFormatter(outputFormatter).concurrency(concurrency).timeout(timeout)
                .syn(syn).robust(robust).build();
        PortScanner portScanner = new PortScanner(cxt);
        try {
            String output = InvocationResponse.invoke(invocationRequest, portScanner);
            System.out.println(output);
        } catch (InvocationTargetException | IllegalAccessException exception) {
            LOGGER.error("Error invoking command method [{}#{}] with parameters {}: {}",
                    invocationRequest.getMethod().getClass().getName(),
                    invocationRequest.getMethod().getName(), invocationRequest.getMethod().getParameters(),
                    exception.getMessage());
        }
    }

    private static int initTimeout(Map<String, String> flags) {
        int timeout = Integer.parseInt(flags.getOrDefault(TIMEOUT.getFullName(), String.valueOf(0)));
        return timeout >= TIME_OUT_LOW_LIMIT && timeout <= TIME_OUT_HIGH_LIMIT ? timeout : DEFAULT_TIMEOUT;
    }

    private static int initConcurrency(Map<String, String> flags) {
        int concurrency = Integer.parseInt(flags.getOrDefault(CONCURRENCY.getFullName(), String.valueOf(PortScanner.DEFAULT_CONCURRENCY)));
        return concurrency >= 2 && concurrency <= 300 ? concurrency : DEFAULT_CONCURRENCY;
    }

    private static boolean initSneakyLink(Map<String, String> flags) {
        return Boolean.parseBoolean(flags.getOrDefault(SYN.getFullName(), String.valueOf(false)));
    }

    private static boolean initRobust(Map<String, String> flags) {
        return Boolean.parseBoolean(flags.getOrDefault(ROBUST.getFullName(), String.valueOf(false)));
    }

    private static OutputFormatter initOutputFormatter(Map<String, String> flags) {
        String format = flags.getOrDefault(FORMAT.getFullName(), "txt");
        if (format.equals("txt")) {
            return new HumanReadableFormatter();
        }
        return new JsonFormatter();
    }
}
