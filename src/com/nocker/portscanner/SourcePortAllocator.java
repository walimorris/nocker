package com.nocker.portscanner;

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
    private static final int MIN = 49152;
    private static final int MAX = 65535;

    private static final AtomicInteger current = new AtomicInteger(MIN);

    private SourcePortAllocator() {}

    public static synchronized int next() {
        return current.getAndUpdate(port -> port >= MAX ? MIN : port + 1);
    }
}
