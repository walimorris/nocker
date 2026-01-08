package com.nocker.command;

import com.nocker.portscanner.wildcard.CidrWildcard;
import com.nocker.portscanner.wildcard.PortWildcard;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.Inet4Address;
import java.util.LinkedHashMap;

class ArgumentConverterTest {

    @Test
    void convertToObjectArray() {
        LinkedHashMap<String, Class> parameters = new LinkedHashMap<>();
        parameters.put("host", String.class);
        parameters.put("port", PortWildcard.class);

        LinkedHashMap<String, String> arguments = new LinkedHashMap<>();
        arguments.put("host", "localhost");
        arguments.put("port", "8080-8088");

        Object[] objs = ArgumentConverter.convertToObjectArray(parameters, arguments);

        Assertions.assertEquals(2, objs.length);
        Assertions.assertEquals("localhost", objs[0]);
        Assertions.assertEquals(PortWildcard.class, objs[1].getClass());
    }

    @Test
    void convertToObjectArrayThrowsIllegalArgumentExceptionKeySizeMismatch() {
        LinkedHashMap<String, Class> parameters = new LinkedHashMap<>();
        parameters.put("host", String.class);
        parameters.put("port", CidrWildcard.class);
        parameters.put("server", Inet4Address.class);

        LinkedHashMap<String, String> arguments = new LinkedHashMap<>();
        arguments.put("port", "127.0.0.1/24");
        arguments.put("server", "127.0.0.1");

        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ArgumentConverter.convertToObjectArray(parameters, arguments);
        });

        System.out.println(exception.getMessage());
        Assertions.assertEquals("Keys mismatch: parameter keys " +
                parameters.keySet() + " and argument keys " + arguments.keySet() + " must match", exception.getMessage());

    }

    @Test
    void convertToObjectArrayThrowsIllegalArgumentExceptionKeyValueMismatch() {
        LinkedHashMap<String, Class> parameters = new LinkedHashMap<>();
        parameters.put("host", String.class);
        parameters.put("port", Integer.class);
        parameters.put("server", Inet4Address.class);

        LinkedHashMap<String, String> arguments = new LinkedHashMap<>();
        arguments.put("host", "localhost");
        arguments.put("ports", "8080-8088");
        arguments.put("server", "scanme.voyager1");

        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ArgumentConverter.convertToObjectArray(parameters, arguments);
        });

        System.out.println(exception.getMessage());
        Assertions.assertEquals("Keys mismatch: parameter keys " +
                parameters.keySet() + " and argument keys " + arguments.keySet() + " must match", exception.getMessage());

    }

    @Test
    void getArgumentNamesAndTypesReturns() {
        LinkedHashMap<String, String> arguments = new LinkedHashMap<>();
    }
}