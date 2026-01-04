package com.nocker.command;

import com.nocker.portscanner.PortScanner;
import com.nocker.portscanner.annotation.arguments.Host;
import com.nocker.portscanner.annotation.arguments.Hosts;
import com.nocker.portscanner.annotation.arguments.Port;
import com.nocker.portscanner.annotation.arguments.Ports;
import com.nocker.portscanner.annotation.commands.CIDRScan;
import com.nocker.portscanner.annotation.commands.Scan;
import com.nocker.portscanner.wildcard.PortWildcard;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MethodResolverTest {
    private static final Map<String, Class> TEST_METHOD_CLASS_HASH;

    private final Method methodWithTwoParameters1 = this.getClass().getMethod("thisMethodContainsDefaultNockerArgs", String.class, int.class);
    private final Method methodWithTwoParameters2 = this.getClass().getMethod("thisMethodContainsDefaultNockerArgs", String.class, PortWildcard.class);
    private final Method methodWithTwoParameters3 = this.getClass().getMethod("thisMethodContainsSuppliedNockerArgs", String.class, int.class);
    private final Method methodWithThreeParameters = this.getClass().getMethod("thisMethodContainsNockerAndNonNockerArgs", String.class, int.class, int.class);
    private final Method methodWithTwoNoNockerParameters = this.getClass().getMethod("thisMethodDoesNotContainNockerArgs", String.class, int.class);

    static {
        TEST_METHOD_CLASS_HASH = new HashMap<>();
        TEST_METHOD_CLASS_HASH.put("scan", PortScanner.class);
        TEST_METHOD_CLASS_HASH.put("cidr-scan", PortScanner.class);
    }

    MethodResolverTest() throws NoSuchMethodException {
    }

    @Test
    void getNockerParameterNamesAndTypesWithNoNockerArgs() {
        // this method has parameters, however they are both non-nocker args
        // Therefore, the expected result is 0
        LinkedHashMap<String, Class> result1 = MethodResolver.getNockerParameterNamesAndTypes(methodWithTwoNoNockerParameters);
        LinkedHashMap<String, Class> result2 = MethodResolver.getNockerParameterNamesAndTypes(methodWithTwoParameters1);
        LinkedHashMap<String, Class> result3 = MethodResolver.getNockerParameterNamesAndTypes(methodWithTwoParameters2);
        LinkedHashMap<String, Class> result4 = MethodResolver.getNockerParameterNamesAndTypes(methodWithTwoParameters3);

        // this method has three parameters, but only two nocker parameters.
        // Therefore, the expected result is 2 of the parameters are returned
        LinkedHashMap<String, Class> result5 = MethodResolver.getNockerParameterNamesAndTypes(methodWithThreeParameters);
        assertEquals(0, result1.size());
        assertEquals(2, result2.size());
        assertEquals(2, result3.size());
        assertEquals(2, result4.size());
        assertEquals(2, result5.size());
    }

    @Test
    void filterMethodsFromCommand() {
        // This test takes all methods in this Test Class and filters the declared methods by the given
        // command. See the declared methods with @Scan, @CIDRScan annotations.
        String commandMethod1 = "scan";
        String commandMethod2 = "cidr-scan";

        List<Method> scanMethods = Collections.singletonList(methodWithTwoParameters1);
        List<Method> cidrScanMethods = Collections.singletonList(methodWithTwoParameters2);

        List<Method> scanMethodsResults = MethodResolver.filterMethodsFromCommand(commandMethod1, this.getClass());
        List<Method> cidrScanMethodsResults = MethodResolver.filterMethodsFromCommand(commandMethod2, this.getClass());

        assertEquals(1, scanMethodsResults.size());
        assertEquals(1, scanMethodsResults.size());
        assertEquals(scanMethods, scanMethodsResults);
        assertEquals(cidrScanMethods, cidrScanMethodsResults);
    }

    @Test
    void filterMethodsByParameterCount() {
        List<Method> methods = Arrays.asList(methodWithTwoParameters1, methodWithTwoParameters2, methodWithTwoParameters3, methodWithThreeParameters);
        List<Method> actual = MethodResolver.filterMethodsByParameterCount(methods, 2);
        assertEquals(4, methods.size());
        assertEquals(3, actual.size());
    }

    @Test
    void containsNockerMethod() {
        String commandMethod1 = "scan";
        String commandMethod2 = "cidr-scan";
        Annotation[] methodWithNockerScanAnnotations = methodWithTwoParameters1.getDeclaredAnnotations();
        Annotation[] methodWithNockerCidrScanAnnotations = methodWithTwoParameters2.getDeclaredAnnotations();
        Annotation[] methodWithoutNockerAnnotations = methodWithTwoParameters3.getDeclaredAnnotations();

        boolean methodWithNockerScanAnnotationResult1 = MethodResolver.containsNockerMethod(methodWithNockerScanAnnotations[0], commandMethod1);
        boolean methodWithNockerCidrScanAnnotationResult1 = MethodResolver.containsNockerMethod(methodWithNockerCidrScanAnnotations[0], commandMethod1);
        boolean methodWithNockerScanAnnotationResult2 = MethodResolver.containsNockerMethod(methodWithNockerScanAnnotations[0], commandMethod2);
        boolean methodWithNockerCidrScanAnnotationResult2 = MethodResolver.containsNockerMethod(methodWithNockerCidrScanAnnotations[0], commandMethod2);

        // annotated with @Scan, 'scan' command given - match
        assertTrue(methodWithNockerScanAnnotationResult1);
        // annotated with @CIDRScan, 'scan' command given - no match
        assertFalse(methodWithNockerCidrScanAnnotationResult1);
        // no annotations - impossible to match given command
        assertEquals(0, methodWithoutNockerAnnotations.length);
        // annotated with @Scan, 'cidr-scan' command given - no match
        assertFalse(methodWithNockerScanAnnotationResult2);
        // annotated with @CIDRScan, 'cidr-scan' command given - match
        assertTrue(methodWithNockerCidrScanAnnotationResult2);
    }

    @Test
    void getAllMethodFromClass() {
        List<Method> methods = MethodResolver.getAllMethodFromClass(this.getClass(), "thisMethodContainsDefaultNockerArgs");
        assertEquals(2, methods.size());
    }

    @Test
    void findClassFromCommandMethodNameReturnsNull() {
        Class resultingClass = MethodResolver.findClassFromCommandMethodName("sus-scan");
        assertNull(resultingClass);
    }

    @Test
    void findClassFromCommandMethodNameThrowsInvalidCommandException() {
        Class clazz = MethodResolver.findClassFromCommandMethodName("cidr-scan-v2");
        assertNull(clazz);
    }

    @Test
    void findClassFromCommandMethodNameSinglePart() {
        // single part means a single method with no '-' to separate parts (i.e., scan vs cidr-scan)
        Class resultingClass = MethodResolver.findClassFromCommandMethodName("scan");
        Class expected = TEST_METHOD_CLASS_HASH.get("scan");
        assertEquals(expected, resultingClass);
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

    @Scan
    public static boolean thisMethodContainsDefaultNockerArgs(@Hosts String hosts, @Port int port) {
        return !hosts.isEmpty() && port > 1;
    }

    @CIDRScan
    public boolean thisMethodContainsDefaultNockerArgs(@Host String host, @Ports PortWildcard ports) {
        return !host.isEmpty() && ports.getHighPort() > ports.getLowPort();
    }

    public boolean thisMethodContainsSuppliedNockerArgs(@Host(name = "theHost") String theHost, @Port(name = "thePort") int thePort) {
        return !theHost.isEmpty() && (thePort > 1 && thePort <= 65536);
    }

    public boolean thisMethodContainsNockerAndNonNockerArgs(@Host String theHost, @Port int thePort,
                                                             int theExcludedPort) {
        return !theHost.isEmpty() && thePort > theExcludedPort;
    }

    public boolean thisMethodDoesNotContainNockerArgs(String host, int port) {
        return !host.isEmpty() && port > 1;
    }
}