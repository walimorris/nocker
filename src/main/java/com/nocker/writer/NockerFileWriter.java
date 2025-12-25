package com.nocker.writer;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Wrapper around {@link BufferedWriter} standard Java class. Only exposing the write and close
 * methods of the class. {@code NockerFileWriter} makes explicit file writing decisions such as
 * appending a new line after each write to the end of a file, rather than writing at the beginning.
 * This fits well in Nocker's multithreaded environment since the standard BufferedWriter is
 * {@code ThreadSafe} by design.
 */
public class NockerFileWriter {
    private final String writePath;
    private BufferedWriter bufferedWriter;

    public NockerFileWriter(String writePath) {
        this.writePath = writePath;

        if (StringUtils.isNotBlank(writePath)) {
            try {
                bufferedWriter = new BufferedWriter(new FileWriter(writePath, true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(String content) {
        if (bufferedWriter != null) {
            try {
                bufferedWriter.write(content);
                bufferedWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void closeWriter() {
        if (bufferedWriter != null) {
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getWritePath() {
        return writePath;
    }
}
