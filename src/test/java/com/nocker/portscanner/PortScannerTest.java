package com.nocker.portscanner;

import com.nocker.cli.PortScannerContext;
import com.nocker.cli.formatter.OutputFormatter;
import com.nocker.portscanner.command.CommandLineInput;
import com.nocker.portscanner.command.InvocationCommand;
import com.nocker.portscanner.report.PortScanReport;
import com.nocker.portscanner.report.PortScanResult;
import com.nocker.portscanner.scheduler.PortScanScheduler;
import com.nocker.portscanner.scheduler.PortScanSchedulerFactory;
import com.nocker.portscanner.scheduler.PortScanSynAckScheduler;
import com.nocker.portscanner.tasks.PortRange;
import com.nocker.portscanner.tasks.PortScanSynAckTask;
import com.nocker.portscanner.wildcard.CidrWildcard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nocker.portscanner.PortScanner.MAX_SCHEDULERS;
import static com.nocker.portscanner.PortState.CLOSED;
import static com.nocker.portscanner.PortState.OPEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PortScannerTest {
    private static PortScannerContext BASIC_CXT;
    private static InvocationCommand BASIC_INVOCATION_COMMAND;
    private static OutputFormatter BASIC_OUTPUT_FORMATTER;
    private static PortScanSchedulerFactory BASIC_SCHEDULER_FACTORY;

    @BeforeAll
    static void setup() {
        BASIC_INVOCATION_COMMAND = Mockito.mock(InvocationCommand.class);
        BASIC_OUTPUT_FORMATTER = Mockito.mock(OutputFormatter.class);
        BASIC_SCHEDULER_FACTORY = Mockito.mock(PortScanSchedulerFactory.class);

        BASIC_CXT = new PortScannerContext.Builder()
                .invocationCommand(BASIC_INVOCATION_COMMAND)
                .nockerFileWriter(null).outputFormatter(BASIC_OUTPUT_FORMATTER)
                .concurrency(100)
                .schedulerFactory(BASIC_SCHEDULER_FACTORY)
                .timeout(5).syn(false).robust(false).build();
    }

    @Test
    void testCidrScanWithHostsWildcard() {
        CidrWildcard cidrWildcard = new CidrWildcard("127.0.0.254/24");
        PortScanner portScanner = new PortScanner(BASIC_CXT);
        PortScanSynAckScheduler mockScheduler = Mockito.mock(PortScanSynAckScheduler.class);
        PortScanReport mockPortScanReport = Mockito.mock(PortScanReport.class);
        when(BASIC_SCHEDULER_FACTORY.create()).thenReturn(mockScheduler);
        when(mockScheduler.shutdownAndCollect(any(AtomicInteger.class)))
                .thenReturn(mockPortScanReport);

        portScanner.cidrScan(cidrWildcard);
        verify(mockScheduler, times(1)).shutdownAndCollect(any(AtomicInteger.class));
        verify(mockScheduler).shutdownAndCollect(argThat(count -> count.get() > 0));
        // the basic context is not sneaky
        verify(mockScheduler, atLeast(1)).submit(isA(PortScanSynAckTask.class));
        // this is not flagged with robust and sends a human-readable summary
        verify(mockPortScanReport, times(1)).getSummary();
    }

    @Test
    void testGeneratePortScanReportFromPortScanResults() throws UnknownHostException {
        PortScanner portScanner = new PortScanner(BASIC_CXT);
        UUID taskId = UUID.randomUUID();
        Inet4Address address = (Inet4Address) Inet4Address.getLocalHost();
        PortScanResult result1 = new PortScanResult(null, taskId, address, 8080, OPEN, 1);
        PortScanResult result2 = new PortScanResult(null, taskId, address, 8081, OPEN, 1);
        PortScanResult result3 = new PortScanResult(null, taskId, address, 8082, CLOSED, 2);
        PortScanResult result4 = new PortScanResult(null, taskId, address, 8083, CLOSED, 1);
        List<PortScanResult> results = Arrays.asList(result1, result2, result3, result4);

        String command = "nocker scan --host=127.0.0.1 --ports=8080,8081,8082,8083";
        CommandLineInput CLI = Mockito.mock(CommandLineInput.class);
        when(BASIC_INVOCATION_COMMAND.getCommandLineInput()).thenReturn(CLI);
        when(CLI.getCommand()).thenReturn(command);

        PortScanReport report = portScanner.generatePortScanReportFromPortScanResults(results);
        Set<Integer> openPorts = Sets.newSet(8080, 8081);

        assertNotNull(report);
        assertNull(report.getPortScanScheduler());
        assertNotNull(report.getSummary());
        assertEquals(4, report.getResults().size());
        assertEquals(2, report.getSummary().getClosedPortsCount());
        assertEquals(2, report.getSummary().getOpenPortsCount());
        assertEquals(4, report.getSummary().getTotalPortsScanned());
        assertEquals(5, report.getSummary().durationMillis());
        assertTrue(report.getSummary().getOpenHostPorts().containsKey(address.getHostName()));
        assertEquals(command, report.getSummary().getInvocationCommand()
                .getCommandLineInput().getCommand());
        assertEquals(openPorts, report.getSummary().getOpenHostPorts().get(address.getHostName()));
    }

    @Test
    void testSpawnSchedulersReturnsRequestedSize() {
        PortScanner portScanner = new PortScanner(BASIC_CXT);
        List<PortScanScheduler> schedulers = portScanner.spawnSchedulers(3);
        assertEquals(3, schedulers.size());
    }

    @Test
    void testSpawnSchedulersReturnsMaxSchedulersSizeAndNotRequestedSize() {
        PortScanner portScanner = new PortScanner(BASIC_CXT);
        int requestedSize = MAX_SCHEDULERS + 25;
        List<PortScanScheduler> schedulers = portScanner.spawnSchedulers(requestedSize);
        assertNotEquals(requestedSize, schedulers.size());
        assertEquals(MAX_SCHEDULERS, schedulers.size());
    }

    @Test
    void testSumSequentialDuration() throws UnknownHostException {
        PortScanner portScanner = new PortScanner(BASIC_CXT);
        Inet4Address address = (Inet4Address) Inet4Address.getLocalHost();
        List<PortScanResult> results = Arrays.asList(
                new PortScanResult(null, null, address, 8080, CLOSED, 1),
                new PortScanResult(null, null, address, 8081, CLOSED, 2),
                new PortScanResult(null, null, address, 8082, OPEN, 2),
                new PortScanResult(null, null, address, 8083, CLOSED, 1)
        );
        long actualDuration = portScanner.sumSequentialDuration(results);
        assertEquals(6, actualDuration);
    }

    @Test
    void testGetChunksValidScenario() {
        PortScanner portScanner = new PortScanner(BASIC_CXT);

        int startPort = 1;
        int endPort = 100;
        int batchSize = 20;

        List<PortRange> chunks = portScanner.getChunks(startPort, endPort, batchSize);

        assertEquals(5, chunks.size());
        assertEquals(new PortRange(1, 20).toString(), chunks.get(0).toString());
        assertEquals(new PortRange(21, 40).toString(), chunks.get(1).toString());
        assertEquals(new PortRange(41, 60).toString(), chunks.get(2).toString());
        assertEquals(new PortRange(61, 80).toString(), chunks.get(3).toString());
        assertEquals(new PortRange(81, 100).toString(), chunks.get(4).toString());
    }

    @Test
    void testGetChunksValidScenarioSinglePortScan() {
        PortScanner portScanner = new PortScanner(BASIC_CXT);

        int startPort = 4331;
        int endPort = 4332;
        int batchSize = 100;

        List<PortRange> chucks = portScanner.getChunks(startPort, endPort, batchSize);

        assertEquals(1, chucks.size());
        assertEquals(new PortRange(4331, 4332).toString(), chucks.get(0).toString());
    }

    @Test
    void testGetChunksSingleBatch() {
        PortScanner portScanner = new PortScanner(BASIC_CXT);

        int startPort = 1;
        int endPort = 50;
        int batchSize = 100;

        List<PortRange> chunks = portScanner.getChunks(startPort, endPort, batchSize);

        assertEquals(1, chunks.size());
        assertEquals(new PortRange(1, 50).toString(), chunks.get(0).toString());
    }

    @Test
    void testGetChunksRemainingPorts() {
        PortScanner portScanner = new PortScanner(BASIC_CXT);

        int startPort = 1;
        int endPort = 55;
        int batchSize = 20;

        List<PortRange> chunks = portScanner.getChunks(startPort, endPort, batchSize);

        assertEquals(3, chunks.size());
        assertEquals(new PortRange(1, 20).toString(), chunks.get(0).toString());
        assertEquals(new PortRange(21, 40).toString(), chunks.get(1).toString());
        assertEquals(new PortRange(41, 55).toString(), chunks.get(2).toString());
    }

    @Test
    void testGetChunksInvalidRange() {
        PortScanner portScanner = new PortScanner(BASIC_CXT);

        int startPort = 100;
        int endPort = 50;
        int batchSize = 10;

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            portScanner.getChunks(startPort, endPort, batchSize);
        });

        assertEquals("ending port can not be less than starting port", exception.getMessage());
    }
}