package com.nocker.cli.formatter;

import com.nocker.portscanner.report.ScanSummary;

import java.io.PrintStream;
import java.util.List;

public class HumanReadableFormatter implements OutputFormatter {

    @Override
    public void write(List<?> report, PrintStream out) {

    }

    @Override
    public void write(Object obj, PrintStream out) {

    }

    @Override
    public void write(ScanSummary scanSummary, PrintStream out) {
        out.println(scanSummary.toSummary());
    }
}
