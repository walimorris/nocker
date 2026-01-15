package com.nocker.cli;

import com.nocker.cli.formatter.OutputFormatter;
import com.nocker.portscanner.command.InvocationRequest;
import com.nocker.portscanner.scheduler.PortScanSchedulerFactory;
import com.nocker.writer.NockerFileWriter;
import com.nocker.portscanner.PortScanner;

/**
 * {@code PortScannerContext} represents an immutable execution context for a port scan.
 * It encapsulates all configuration, execution policies, and runtime dependencies required
 * by the {@link PortScanner} to perform a scan.
 * <p>
 * Instances of this class are constructed using a builder to ensure controlled and
 * explicit initialization. The context may include invocation metadata, output handling
 * components, concurrency and timeout policies, and feature flags that influence
 * scanning behavior.
 * <p>
 * This class is intended to be created once per scan invocation and passed through
 * the scanning pipeline as a read-only object.
 *
 * @see PortScanner
 * @see NockerCommandLineInterface
 *
 * @author Wali Morris
 */

public class PortScannerContext {
    private final InvocationRequest invocationRequest;
    private final NockerFileWriter nockerFileWriter;
    private final OutputFormatter outputFormatter;
    private final PortScanSchedulerFactory schedulerFactory;
    private final int concurrency;
    private final int timeout;
    private final boolean syn;
    private final boolean robust;

    PortScannerContext(Builder builder) {
        this.invocationRequest = builder.invocationRequest;
        this.nockerFileWriter = builder.nockerFileWriter;
        this.outputFormatter = builder.outputFormatter;
        this.schedulerFactory = builder.schedulerFactory;;
        this.concurrency = builder.concurrency;
        this.timeout = builder.timeout;
        this.syn = builder.syn;
        this.robust = builder.robust;
    }

    public InvocationRequest getInvocationCommand() {
        return invocationRequest;
    }

    public NockerFileWriter getNockerFileWriter() {
        return nockerFileWriter;
    }

    public OutputFormatter getOutputFormatter() {
        return outputFormatter;
    }

    public PortScanSchedulerFactory getSchedulerFactory() {
        return schedulerFactory;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isSyn() {
        return syn;
    }

    public boolean isRobust() {
        return robust;
    }

    public static class Builder {
        private InvocationRequest invocationRequest;
        private NockerFileWriter nockerFileWriter;
        private OutputFormatter outputFormatter;
        private PortScanSchedulerFactory schedulerFactory;
        private int concurrency;
        private int timeout;
        private boolean syn;
        private boolean robust;

        public Builder invocationCommand(InvocationRequest invocationRequest) {
            this.invocationRequest = invocationRequest;
            return this;
        }

        public Builder nockerFileWriter(NockerFileWriter nockerFileWriter) {
            this.nockerFileWriter = nockerFileWriter;
            return this;
        }

        public Builder outputFormatter(OutputFormatter outputFormatter) {
            this.outputFormatter = outputFormatter;
            return this;
        }

        public Builder schedulerFactory(PortScanSchedulerFactory schedulerFactory) {
            this.schedulerFactory = schedulerFactory;
            return this;
        }

        public Builder concurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder syn(boolean syn) {
            this.syn = syn;
            return this;
        }

        public Builder robust(boolean robust) {
            this.robust = robust;
            return  this;
        }

        public PortScannerContext build() {
            return new PortScannerContext(this);
        }
    }
}
