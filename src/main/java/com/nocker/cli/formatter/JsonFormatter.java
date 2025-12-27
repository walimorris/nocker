package com.nocker.cli.formatter;

import com.nocker.cli.CommandLineUtil;

import java.io.PrintStream;
import java.util.List;

public class JsonFormatter implements OutputFormatter {

    @Override
    public void write(List<?> report, PrintStream out) {
        String json = CommandLineUtil.jsonifyList(report);
        write(json, out);
    }

    @Override
    public void write(Object obj, PrintStream out) {
        String json = CommandLineUtil.jsonify(obj);
        write(json, out);
    }
}
