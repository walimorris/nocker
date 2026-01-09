package com.nocker.portscanner;

import com.nocker.cli.PortScannerContext;
import com.nocker.cli.formatter.HumanReadableFormatter;
import com.nocker.portscanner.command.CommandLineInput;
import com.nocker.portscanner.command.CommandMethod;
import com.nocker.portscanner.command.InvocationCommand;
import com.nocker.portscanner.scheduler.PortScanSchedulerFactory;
import com.nocker.portscanner.scheduler.PortScanSynAckSchedulerFactory;
import com.nocker.portscanner.wildcard.PortWildcard;
import com.nocker.writer.NockerFileWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class PortScannerIT {
    private final static DockerImageName ALPINE_SOCAT_IMAGE = DockerImageName.parse("alpine-socat");

    @Container
    private final static GenericContainer<?> testContainer = new GenericContainer<>(ALPINE_SOCAT_IMAGE)
            .withCommand("sh", "-c", "apk add --no-cache socat && " +
                            "socat TCP-LISTEN:8082,fork EXEC:'/bin/cat' & " +
                            "socat TCP-LISTEN:8087,fork EXEC:'/bin/cat' & " +
                            "wait")
            .withExposedPorts(8082, 8087)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)));

    private static PortScanSchedulerFactory schedulerFactory;
    private static PortScanner portScanner;

    @BeforeAll
    static void setupScanner() throws NoSuchMethodException, IOException {
        String host = testContainer.getHost();
        int p1 = testContainer.getMappedPort(8082);
        int p2 = testContainer.getMappedPort(8087);

        int start = Math.min(p1, p2) - 2;
        int end   = Math.max(p1, p2) + 2;
        Path tempDir = Files.createTempDirectory("nocker-output-");
        Path tempFile = tempDir.resolve("report.json");
        Method method = PortScanner.class.getMethod("scan", String.class, PortWildcard.class);
        CommandMethod commandMethod = new CommandMethod("scan", "scan", method);

        CommandLineInput commandLineInput = new CommandLineInput(
                "nocker scan --host=" + host + " --ports=" + start + "-" + end + " -c 2 -t 50",
                commandMethod, null, null
        );
        InvocationCommand invocationCommand = new InvocationCommand(commandLineInput, method, new Object[]{});
        schedulerFactory = new PortScanSynAckSchedulerFactory(invocationCommand);
        PortScannerContext cxt = new PortScannerContext.Builder().invocationCommand(invocationCommand)
                .nockerFileWriter(new NockerFileWriter(tempFile.toAbsolutePath().toString()))
                .outputFormatter(new HumanReadableFormatter()).concurrency(2).timeout(50).robust(false)
                .syn(false).schedulerFactory(schedulerFactory).build();
        portScanner = new PortScanner(cxt);
    }

    @AfterAll
    static void cleanup() {
    }

    @Test
    void scanHostAndPortWildcard() {
        String host = testContainer.getHost();
        String ip = PortScannerUtil.getHostInet4Address(host).getHostAddress();
        int p1 = testContainer.getMappedPort(8082);
        int p2 = testContainer.getMappedPort(8087);

        int start = Math.min(p1, p2) - 2;
        int end = Math.max(p1, p2) + 2;

        portScanner.scan(host, new PortWildcard(start + "-" + end));
        Set<Integer> openPorts = portScanner.getReport()
                .getSummary()
                .getOpenHostPorts()
                .get(ip);

        // extrapolated the scan range around the known open ports (2 exposed ports, padded by 2 each side)
        // in this case we know the total scanned ports to be 6
        assertEquals(6, portScanner.getReport().getSummary().getTotalPortsScanned());
        assertTrue(openPorts.contains(p1));
        assertTrue(openPorts.contains(p2));
    }
}