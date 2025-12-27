package com.nocker.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nocker.portscanner.PortScanResult;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class CommandLineUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineUtil.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentObjectsWith(new DefaultIndenter("    ", "\n"));
        prettyPrinter.indentArraysWith(new DefaultIndenter("    ", "\n"));
        mapper.setDefaultPrettyPrinter(prettyPrinter);
    }

    private CommandLineUtil() {
        throw new AssertionError("CommandLineUtil cannot be instantiated");
    }

    /**
     * Converts a list of {@link PortScanResult} objects into a JSON string representation.
     * If the list is empty, an exception is thrown. If JSON processing fails, the error will
     * be logged, and the method returns null.
     *
     * @param portScanResults a non-empty list of {@link PortScanResult} objects to be converted to JSON
     * @return a JSON string representation of the provided port scan results, or null if JSON processing fails
     * @throws IllegalStateException if the provided list of port scan results is empty
     */
    public static String jsonifyPortscanResults(List<PortScanResult> portScanResults) {
        if (ObjectUtils.isNotEmpty(portScanResults)) {
            try {
                return mapper.writer(mapper.getSerializationConfig().getDefaultPrettyPrinter())
                        .writeValueAsString(portScanResults);
            } catch (JsonProcessingException e) {
                List<PortScanResult> truncatedResultsList = portScanResults.subList(0, 3);
                LOGGER.error("Error processing portscan results to json: {}...: ", truncatedResultsList);
                return null;
            }
        }
        throw new IllegalStateException("Cannot jsonify empty portscan results list");
    }

    public static String jsonifyPortScanResult(PortScanResult result) {
        if (ObjectUtils.isNotEmpty(result)) {
            try {
                return mapper.writeValueAsString(result);
            } catch (JsonProcessingException e) {
                LOGGER.error("Error processing port scan result to json {}: ", result);
                return null;
            }
        }
        throw new IllegalStateException("Cannot jsonify empty portscan result");
    }
}
