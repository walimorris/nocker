package com.nocker.cli.formatter;

import com.nocker.portscanner.PortScanResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public interface OutputFormatter {
    Logger LOGGER = LoggerFactory.getLogger(OutputFormatter.class);

    void write(List<PortScanResult> report, PrintStream out);
    void write(PortScanResult report, PrintStream out);

    default void write(String content, PrintStream out) {
        try {
            if (StringUtils.isNotBlank(content)) {
                out.write(content.getBytes());
            }
        } catch (IOException e) {
            LOGGER.error("cannot parse or write content: {}", e.getMessage());
        }
    }
}
