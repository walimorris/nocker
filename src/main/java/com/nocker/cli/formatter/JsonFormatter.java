package com.nocker.cli.formatter;

import com.nocker.cli.CommandLineUtil;
import com.nocker.portscanner.PortScanResult;

import java.io.PrintStream;
import java.util.List;

public class JsonFormatter implements OutputFormatter {

    @Override
    public void write(List<PortScanResult> report, PrintStream out) {
        String json = CommandLineUtil.jsonifyPortscanResults(report);
        write(json, out);
    }

    @Override
    public void write(PortScanResult report, PrintStream out) {
        String json = CommandLineUtil.jsonifyPortScanResult(report);
        write(json, out);
    }
}
