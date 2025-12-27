package com.nocker.cli.formatter;

import org.apache.commons.lang3.StringUtils;

import java.io.PrintStream;
import java.util.List;

public interface OutputFormatter {
    /**
     * Writes the provided report data to the specified output stream.
     *
     * @param report the data to be written, represented as a list of objects
     * @param out the output stream where the report data will be written
     */
    void write(List<?> report, PrintStream out);

    /**
     * Writes the provided object to the specified output stream.
     *
     * @param obj the object to be written
     * @param out the output stream where the object will be written
     */
    void write(Object obj, PrintStream out);

    /**
     * Writes the provided string content to the specified output stream.
     * The content is printed only if it is not blank.
     *
     * @param content the string content to be written
     * @param out the output stream where the content will be written
     */
    default void write(String content, PrintStream out) {
        if (StringUtils.isNotBlank(content)) {
            out.println(content);
        }
    }
}
