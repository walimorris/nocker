package com.nocker.portscanner.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class HostIdentityTest {

    @Test
    void getHostIdentityNonBuilder() throws UnknownHostException {
        Inet4Address inet4Address = (Inet4Address) Inet4Address.getLocalHost();
        String hostAddress = inet4Address.getHostAddress();
        String hostName = inet4Address.getHostName();

        HostIdentity hostIdentity = new HostIdentity(inet4Address, hostAddress, hostName);

        assertEquals(inet4Address, hostIdentity.getHostInet4Address());
        assertEquals(hostAddress, hostIdentity.getHostAddress());
        assertEquals(hostName, hostIdentity.getHostname());
    }

    @Test
    void getHostIdentityNonBuilderThrowsNull() throws UnknownHostException {
        Inet4Address inet4Address = (Inet4Address) Inet4Address.getLocalHost();
        String hostAddress = inet4Address.getHostAddress();

        Exception exception = Assertions.assertThrows(NullPointerException.class, () -> {
            new HostIdentity(inet4Address, hostAddress, null);
        });

        assertEquals("hostname cannot be null.", exception.getMessage());
    }

    @Test
    void getHostIdentityBuilder() throws UnknownHostException {
        Inet4Address inet4Address = (Inet4Address) Inet4Address.getLocalHost();
        String hostAddress = inet4Address.getHostAddress();
        String hostName = inet4Address.getHostName();

        HostIdentity hostIdentity = new HostIdentity.Builder()
                .hostInet4Address(inet4Address)
                .hostAddress(hostAddress)
                .hostname(hostName).build();

        assertEquals(inet4Address, hostIdentity.getHostInet4Address());
        assertEquals(hostAddress, hostIdentity.getHostAddress());
        assertEquals(hostName, hostIdentity.getHostname());
    }

    @Test
    void getHostIdentityBuilderThrowsNull() throws UnknownHostException {
        Inet4Address inet4Address = (Inet4Address) Inet4Address.getLocalHost();
        String hostAddress = inet4Address.getHostAddress();

        Exception exception = Assertions.assertThrows(NullPointerException.class, () -> {
            new HostIdentity.Builder()
                    .hostInet4Address(inet4Address)
                    .hostAddress(hostAddress)
                    .hostname(null).build();
        });

        assertEquals("HostIdentity hostname cannot be null", exception.getMessage());
    }
}