package com.nocker.command;

import com.nocker.portscanner.wildcard.CidrWildcard;
import com.nocker.portscanner.wildcard.PortWildcard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArgumentConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentConverter.class);

    private static final Class<?>[] validTypes = new Class[] {
            String.class,
            int.class,
            Integer.class,
            List.class,
            CidrWildcard.class,
            PortWildcard.class
    };

    private ArgumentConverter() {
        throw new AssertionError("Cannot instantiate instance of final class" + getClass().getName());
    }

    /**
     * Converts a map of string arguments into an array of objects, based on the types specified
     * in a corresponding parameter map. The keys of the parameters map and the arguments map
     * must match exactly.
     *
     * @param parameters a map where the keys are parameter names and the values are the expected {@code Class} types
     *                   for the arguments
     * @param args a map where the keys are argument names and the values are the string representations of the arguments' value
     * @return an array of objects, where each object corresponds to the converted value of the argument string
     *         into the type specified in the parameters map
     * @throws IllegalArgumentException if the keys of the parameters map and the arguments map do not match
     */
    public static Object[] convertToObjectArray(LinkedHashMap<String, Class> parameters, LinkedHashMap<String, String> args) {
        if (!hasIdenticalStringKeys(parameters, args)) {
            LOGGER.error("Keys mismatch: parameter keys {} do not match argument keys {}", parameters, args);
            throw new IllegalArgumentException("Keys mismatch: parameter keys " +
                    parameters.keySet() + " and argument keys " + args.keySet() + " must match");
        }
        return parameters.entrySet()
                .stream()
                .map(entry -> convert(args.get(entry.getKey()), entry.getValue()))
                .toArray(Object[]::new);
    }

    /**
     * Compares the keys of two given maps to determine if they are identical.
     * Specifically, it checks whether the set of keys in the first map is
     * equivalent to the set of keys in the second map.
     *
     * @param expected the first map with string keys to compare
     * @param actual the second map with string keys to compare
     * @return {@code true} if both maps have identical keys, {@code false} otherwise
     */
    private static boolean hasIdenticalStringKeys(Map<String, ?> expected, Map<String, ?> actual) {
        return expected.keySet().equals(actual.keySet());
    }

    /**
     * Processes a map of argument names and their corresponding string value representations,
     * and converts the value strings into their respective {@link Class} object type.
     * The resulting map maintains the order of the input entries.
     * <p>
     * Arguments are valid strings. However, nocker has some arguments that should
     * be parsed into it's nocker class type. This helps the CommandEngine decide
     * on the correct method, given methods that share the same arg names.
     * <p>
     *     Review the following nocker command:
     *     <pre>
     *         {@code nocker scan --host=localhost --ports=8080-8088}
     *     </pre>
     * </p>
     *
     * @param arguments a map where containing keys and their values represented as strings
     * @return a {@code LinkedHashMap} where the keys are the argument names and the values are the corresponding {@code Class} objects
     *         derived from the key's value
     */
    public static LinkedHashMap<String, Class> getArgumentNamesAndTypes(LinkedHashMap<String, String> arguments) {
        LinkedHashMap<String, Class> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            result.put(entry.getKey(), convert(entry.getValue()));
        }
        return result;
    }

    private static Class convert(String value) {
        try {
            Integer.parseInt(value);
            return Integer.class;
        } catch (NumberFormatException e) {
            if (isListCommaDelimited(value)) {
                return List.class;
            }
            if (isValidCIDRWildcard(value)) {
                return CidrWildcard.class;
            }
            if (isValidPortWildcard(value)) {
                return PortWildcard.class;
            }
            return String.class;
        }
    }

    private static Object convert(String value, Class<?> type) {
        if (type == String.class) {
            return value;
        }
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        }
        if (type == List.class) {
            if (isListCommaDelimited(value)) {
                return Arrays.asList(value.split(","));
            }
        }
        if (type == CidrWildcard.class) {
            return new CidrWildcard(value);
        }
        if (type == PortWildcard.class) {
            return new PortWildcard(value);
        }
        LOGGER.error("Unsupported argument type: {}", type);
        throw new IllegalArgumentException("must be one of valid types: " + Arrays.toString(validTypes));
    }

    private static boolean isListCommaDelimited(String str) {
        return str.trim().contains(",");
    }

    private static boolean isValidCIDRWildcard(String value) {
        try {
            new CidrWildcard(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private static boolean isValidPortWildcard(String value) {
        try {
            new PortWildcard(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}
