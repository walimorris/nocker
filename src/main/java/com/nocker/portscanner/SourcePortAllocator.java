package com.nocker.portscanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@code SourcePortAllocator} utilizes ephemeral ports. An {@code Ephemeral port}
 * is a communication endpoint of a TCP layer protocol of the IP suite that is used
 * for short-lived ports. Short-lived ports are allocated automatically within a given
 * predefined range of port numbers. The allocation of an ephemeral port is temporary
 * and valid for the duration of the communication session.
 * <p>
 * The Internet Assigned Numbers Authority (IANA)
 * <a href="https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml">RFC6335</a>
 * states dynamic ports (49152 - 65535) are not assigned and, therefore, the reason
 * {@code nocker} utilizes this range of ports.
 */
public class SourcePortAllocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourcePortAllocator.class);

    private final int minPort;
    private final int maxPort;
    private final AtomicInteger c;

    public static final int MIN = 49152;
    public static final int MAX = 65535;

    public SourcePortAllocator(int min, int max) {
        if (min >= max || min < MIN || max > MAX) {
            LOGGER.error("Invalid port allocation range: [{} - {}] ", min, max);
            throw new IllegalArgumentException("Source port allocation must be in a valid range: [" +
                    MIN + " - " + MAX + "]");
        }
        this.minPort = min;
        this.maxPort = max;
        this.c = new AtomicInteger(min);
    }

    public int getAndIncrement() {
        return c.getAndUpdate(port -> port >= maxPort ? minPort : port + 1);
    }

    public int getCurrent() {
        return c.get();
    }

    public int getMinPort() {
        return minPort;
    }

    public int getMaxPort() {
        return maxPort;
    }
}
