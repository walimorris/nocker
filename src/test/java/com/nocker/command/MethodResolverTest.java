package com.nocker.command;

import com.nocker.portscanner.annotation.arguments.Host;
import com.nocker.portscanner.annotation.arguments.Hosts;
import com.nocker.portscanner.annotation.arguments.Port;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MethodResolverTest {

    @Test
    void getParameterNamesAndTypes() {
    }

    @Test
    void filterMethodsFromCommand() {
    }

    @Test
    void filterMethodsByParameterCount() {
    }

    @Test
    void getAllCommandMethods() {
    }

    @Test
    void containsNockerMethod() {
    }

    @Test
    void getAllMethodFromClass() {
    }

    @Test
    void findClassFromCommandMethodName() {

    }

    @Test
    void getNockerParameterNamesFromMethodReturnsDefaultNamesFromAnnotation() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("thisMethodContainsDefaultNockerArgs", String.class, int.class);
        Set<String> expected = new HashSet<>();
        expected.add("hosts");
        expected.add("port");

        Set<String> actual = MethodResolver.getNockerParameterNamesFromMethod(method);

        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    @Test
    void getNockerParameterNamesFromMethodOnlyReturnsNockerParams() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("thisMethodContainsNockerAndNonNockerArgs", String.class, int.class, int.class);
        Set<String> expected = new HashSet<>();
        expected.add("host");
        expected.add("port");

        Set<String> actual = MethodResolver.getNockerParameterNamesFromMethod(method);

        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    // Beware: nocker annotation engine supplies the default values of valid parameters. @Host default value is "host"
    // However, users can change the default value of a NockerArg: @Host(name = "theHost"). But keep in mind "theHost"
    // is not a valid NockerArg. So, resolving the correct method may fail. Default nocker annotations should not change,
    // and instead its encouraged to change the name of the natural field. i.e, @Host theHost will be resolved as "host".
    // This is a matter of semantics.
    @Test
    void getNockerParameterNamesFromMethodReturnsSuppliedNamesFromAnnotation() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("thisMethodContainsSuppliedNockerArgs", String.class, int.class);
        Set<String> expected = new HashSet<>();
        expected.add("theHost");
        expected.add("thePort");

        Set<String> actual = MethodResolver.getNockerParameterNamesFromMethod(method);

        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    private boolean thisMethodContainsDefaultNockerArgs(@Hosts String hosts, @Port int port) {
        return !hosts.isEmpty() && port > 1;
    }

    private boolean thisMethodContainsSuppliedNockerArgs(@Host(name = "theHost") String theHost, @Port(name = "thePort") int thePort) {
        return !theHost.isEmpty() && (thePort > 1 && thePort <= 65536);
    }

    private boolean thisMethodContainsNockerAndNonNockerArgs(@Host String theHost, @Port int thePort,
                                                             int theExcludedPort) {
        return !theHost.isEmpty() && thePort > theExcludedPort;
    }
}