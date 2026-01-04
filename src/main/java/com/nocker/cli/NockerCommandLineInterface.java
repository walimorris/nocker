package com.nocker.cli;

import com.nocker.Flag;
import com.nocker.OperatingSystemUtils;
import com.nocker.command.CommandEngine;
import com.nocker.cli.formatter.HumanReadableFormatter;
import com.nocker.cli.formatter.JsonFormatter;
import com.nocker.cli.formatter.OutputFormatter;
import com.nocker.portscanner.PortScanner;
import com.nocker.portscanner.command.CommandLineInput;
import com.nocker.portscanner.command.InvocationCommand;
import com.nocker.writer.NockerFileWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static com.nocker.Flag.*;

// TODO: introduce a flag container. Passing so many individual flags into the PortScanner is ridiculous
public final class NockerCommandLineInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(NockerCommandLineInterface.class);

    public static int run(String[] args) {
        if (OperatingSystemUtils.isMacOs()) {
            // TODO: add a step here to clean up lexical normalization (mainly extra spaces).
            // TODO: Sort of missed the boat on other lexical normalizations such as flag formats
            // but that can be refactored from (.parse()) at a later time.
            CommandLineInput commandLineInput = CommandLineInput.parse(args);
            InvocationCommand invocationCommand = CommandEngine.retrieve(commandLineInput);
            String outPath = invocationCommand.getCommandLineInput()
                    .getFlags()
                    .getOrDefault(Flag.OUT.getFullName(), null);
            try (NockerFileWriter nockerFileWriter = outPath != null ? new NockerFileWriter(outPath) : null) {
                invokeCommand(invocationCommand, nockerFileWriter);
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

    private static void invokeCommand(InvocationCommand invocationCommand, NockerFileWriter nockerFileWriter) {
        Map<String, String> flags = invocationCommand.getCommandLineInput().getFlags();
        int concurrency = initConcurrency(flags);
        int timeout = initTimeout(flags);
        boolean syn = initSneakyLink(flags);
        boolean robust = initRobust(flags);
        OutputFormatter outputFormatter = initOutputFormatter(flags);
        PortScanner portScanner = new PortScanner(invocationCommand, nockerFileWriter, outputFormatter, timeout, concurrency, syn, robust);
        try {
            invocationCommand.getMethod().invoke(portScanner, invocationCommand.getArgs());
        } catch (InvocationTargetException | IllegalAccessException exception) {
            LOGGER.error("Error invoking command method [{}#{}] with parameters {}: {}",
                    invocationCommand.getMethod().getClass().getName(),
                    invocationCommand.getMethod().getName(), invocationCommand.getMethod().getParameters(),
                    exception.getStackTrace());
        }
    }

    private static int initTimeout(Map<String, String> flags) {
        return Integer.parseInt(flags.getOrDefault(TIMEOUT.getFullName(), String.valueOf(0)));
    }

    private static int initConcurrency(Map<String, String> flags) {
        return Integer.parseInt(flags.getOrDefault(CONCURRENCY.getFullName(), String.valueOf(0)));
    }

    private static boolean initSneakyLink(Map<String, String> flags) {
        return Boolean.parseBoolean(flags.getOrDefault(SYN.getFullName(), String.valueOf(false)));
    }

    private static boolean initRobust(Map<String, String> flags) {
        return Boolean.parseBoolean(flags.getOrDefault(ROBUST.getFullName(), String.valueOf(false)));
    }

    // we can make txt the default - for now I want to see json
    private static OutputFormatter initOutputFormatter(Map<String, String> flags) {
        String format = flags.getOrDefault(FORMAT.getFullName(), "json");
        if (format.equals("txt")) {
            return new HumanReadableFormatter();
        }
        return new JsonFormatter();
    }
}
