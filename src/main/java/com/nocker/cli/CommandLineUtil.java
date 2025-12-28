package com.nocker.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
     * Converts a list of {@link <T>} objects into a JSON string representation.
     * If the list is empty, an exception is thrown. If JSON processing fails, the error will
     * be logged, and the method returns null.
     *
     * @param <T> a non-empty list of objects to be converted to JSON
     * @return a JSON string representation of the provided objects, or null if JSON processing fails
     * @throws IllegalStateException if the provided list is empty
     */
    public static <T> String jsonifyList(List<T> values) {
        if (ObjectUtils.isEmpty(values)) {
            throw new IllegalStateException("Cannot jsonify empty or null value");
        }
        try {
            return mapper.writer(mapper.getSerializationConfig().getDefaultPrettyPrinter())
                    .writeValueAsString(values);
        } catch (JsonProcessingException e) {
            List<T> truncatedResultsList = values.subList(0, 3);
            LOGGER.error("Error processing portscan results to json: {}...: ", truncatedResultsList);
            return null;
        }
    }

    public static <T> String jsonify(T value) {
        if (value == null) {
            throw new IllegalStateException("Cannot jsonify null value");
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error processing to json {}: ", value);
            return null;
        }
    }
}
