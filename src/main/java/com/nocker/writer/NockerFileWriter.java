package com.nocker.writer;

import org.apache.commons.lang3.StringUtils;

import java.io.*;

/**
 * {@code NockerFileWriter} provides a utility for writing content to a file in a simple and thread-safe
 * manner. It ensures that content written is appended to the file, and resources are properly managed.
 */
public class NockerFileWriter implements Closeable {
    private final String writePath;
    private final PrintWriter writer;

    public NockerFileWriter(String writePath) throws IOException {
        if (StringUtils.isEmpty(writePath)) {
            throw new IllegalArgumentException("write path cannot be blank");
        }
        this.writePath = writePath;
        this.writer = new PrintWriter(new BufferedWriter(new FileWriter(writePath, true)));
    }

    public void write(String content) {
        if (writer != null) {
            writer.println(content);
        }
    }

    public String getWritePath() {
        return writePath;
    }

    public PrintStream getPrintStream() {
        return new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                writer.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                writer.write(new String(b, off, len));
            }

            @Override
            public void flush() {
                writer.flush();
            }

            @Override
            public void close() {
                writer.close();
            }
        }, true);
    }

    @Override
    public void close() {
        if (writer != null) {
            writer.close();
        }
    }
}
