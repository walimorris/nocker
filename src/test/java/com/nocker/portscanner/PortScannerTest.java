package com.nocker.portscanner;

import com.nocker.cli.PortScannerContext;
import com.nocker.portscanner.tasks.PortRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortScannerTest {

    @Test
    void testGetChunksValidScenario() {
        PortScannerContext cxt = new PortScannerContext.Builder().invocationCommand(null)
                .nockerFileWriter(null).outputFormatter(null).concurrency(100).timeout(5).syn(false)
                .robust(false).build();
        PortScanner portScanner = new PortScanner(cxt);

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
        PortScannerContext cxt = new PortScannerContext.Builder().invocationCommand(null)
                .nockerFileWriter(null).outputFormatter(null).concurrency(200).timeout(5).syn(true)
                .robust(false).build();
        PortScanner portScanner = new PortScanner(cxt);

        int startPort = 4331;
        int endPort = 4332;
        int batchSize = 100;

        List<PortRange> chucks = portScanner.getChunks(startPort, endPort, batchSize);

        assertEquals(1, chucks.size());
        assertEquals(new PortRange(4331, 4332).toString(), chucks.get(0).toString());
    }

    @Test
    void testGetChunksSingleBatch() {
        PortScannerContext cxt = new PortScannerContext.Builder().invocationCommand(null)
                .nockerFileWriter(null).outputFormatter(null).concurrency(100).timeout(5).syn(false)
                .robust(false).build();
        PortScanner portScanner = new PortScanner(cxt);

        int startPort = 1;
        int endPort = 50;
        int batchSize = 100;

        List<PortRange> chunks = portScanner.getChunks(startPort, endPort, batchSize);

        assertEquals(1, chunks.size());
        assertEquals(new PortRange(1, 50).toString(), chunks.get(0).toString());
    }

    @Test
    void testGetChunksRemainingPorts() {
        PortScannerContext cxt = new PortScannerContext.Builder().invocationCommand(null)
                .nockerFileWriter(null).outputFormatter(null).concurrency(100).timeout(5).syn(false)
                .robust(false).build();
        PortScanner portScanner = new PortScanner(cxt);

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
        PortScannerContext cxt = new PortScannerContext.Builder().invocationCommand(null)
                .nockerFileWriter(null).outputFormatter(null).concurrency(100).timeout(5).syn(false)
                .robust(false).build();
        PortScanner portScanner = new PortScanner(cxt);

        int startPort = 100;
        int endPort = 50;
        int batchSize = 10;

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            portScanner.getChunks(startPort, endPort, batchSize);
        });

        assertEquals("ending port can not be less than starting port", exception.getMessage());
    }
}