package com.nocker.cli.formatter;

import com.nocker.cli.CommandLineUtil;
import com.nocker.portscanner.report.ScanSummary;
import com.nocker.portscanner.report.SummaryNode;

import java.io.PrintStream;
import java.util.List;

public class JsonFormatter implements OutputFormatter {

    @Override
    public void write(List<?> report, PrintStream out) {
        String json = CommandLineUtil.jsonifyList(report);
        write(json, out);
    }

    @Override
    public void write(ScanSummary scanSummary, PrintStream out) {
        SummaryNode summaryNode = scanSummary.toSummaryNode();
        String summaryNodeJson = CommandLineUtil.jsonify(summaryNode);
        out.println(summaryNodeJson);
    }

    @Override
    public void write(Object obj, PrintStream out) {
        String json = CommandLineUtil.jsonify(obj);
        write(json, out);
    }
}
