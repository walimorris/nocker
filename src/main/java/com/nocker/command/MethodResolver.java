package com.nocker.command;

import com.nocker.annotations.NockerArg;
import com.nocker.annotations.NockerMethod;
import com.nocker.portscanner.PortScanner;
import com.nocker.portscanner.command.InvalidCommandException;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import static com.nocker.portscanner.command.CommandLineInput.SINGLE_DASH;

/**
 * The {@code MethodResolver} class exposes methods that
 * resolve {@code Nocker} command methods. Overall,
 * formalizes the usage of the Nocker annotation system and,
 * in operation with system rules, implements and ensures
 * legal usage of methods.
 *
 * @author Wali Morris
 */
public class MethodResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodResolver.class);

    private MethodResolver() {
        throw new AssertionError("Cannot instantiate instance of final class" + getClass().getName());
    }

    public static final Map<String, Class> METHOD_CLASS_HASH;

    static {
        METHOD_CLASS_HASH = new HashMap<>();
        METHOD_CLASS_HASH.put("scan", PortScanner.class);
        METHOD_CLASS_HASH.put("cidr-scan", PortScanner.class);
    }

    /**
     * Retrieves the parameter names and their associated types for a given method.
     * This method scans the parameters of the provided method and checks if the
     * parameters annotations are marked with {@link NockerArg}. If so, it
     * extracts the parameter name and type and stores them in a LinkedHashMap.
     *
     * <p>
     * Look at the declaration for {@code com.nocker.portscanner.annotation.arguments.Host}:
     * <pre>
     * {@code
     * @Retention(RetentionPolicy.RUNTIME)
     * @Target(ElementType.PARAMETER)
     * @NockerArg
     * public @interface Host {
     *     String name() default "host";
     *     boolean required() default true;
     * }
     * }
     * </pre>
     * </p>
     *
     * @param method the {@code Method} object whose parameters' names and types are to
     *               be extracted
     * @return a {@code LinkedHashMap} where the keys are parameter names and the values
     *         are the corresponding parameter types, preserving the declaration order of
     *         the method's parameters
     * @throws RuntimeException if an error occurs during annotation parsing or reflection
     * calls
     */
    public static LinkedHashMap<String, Class> getNockerParameterNamesAndTypes(Method method) {
        LinkedHashMap<String, Class> params = new LinkedHashMap<>();
        for (Parameter param : method.getParameters()) {
            for (Annotation at : param.getAnnotations()) {
                Class<? extends Annotation> atype = at.annotationType();
                if (atype.isAnnotationPresent(NockerArg.class)) {
                    try {
                        String name = (String) atype.getMethod("name").invoke(at);
                        Class<?> type = param.getType();
                        params.put(name, type);
                    } catch (Exception e) {
                        LOGGER.error("Error parsing annotation, parameter name and type" +
                                        " from given method: [{}#{}] with parameters {}: {}",
                                method.getClass().getName(), method.getName(),
                                method.getParameters(), e.getStackTrace());
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return params;
    }

    /**
     * Filters and retrieves a list of methods from the specified class that
     * matches the given command method name. Utilizes annotations present on
     * methods to verify compatibility with the specified nocker command method.
     *
     * @param commandMethod the name of the command method to filter the methods by
     * @param clazz the {@code Class} object from which methods need to be retrieved
     *             and filtered
     * @return a {@code List<Method>} containing methods that match the specified
     * command method name
     */
    public static List<Method> filterMethodsFromCommand(String commandMethod, Class clazz) {
        Method[] methods = getAllCommandMethods(clazz);
        List<Method> possibleMethods = new ArrayList<>();
        for (Method method : methods) {
            Annotation[] declaredAnnotations = method.getDeclaredAnnotations();
            if (CollectionUtils.isNotEmpty(Arrays.asList(declaredAnnotations))) {
                // need to force single annotation on method or ensure we're grabbing
                // a nocker method
                Annotation methodAnnotation = declaredAnnotations[0];
                if (containsNockerMethod(methodAnnotation, commandMethod)) {
                    possibleMethods.add(method);
                }
            }
        }
        return possibleMethods;
    }

    /**
     * Filters a list of methods and returns only those methods whose
     * parameter count matches the specified value.
     *
     * @param methods the list of {@code Method} objects to filter
     * @param parameterCount the required number of parameters for methods
     *                      to be included in the result
     * @return a {@code List<Method>} containing methods from the input
     * list that have the specified number of parameters
     */
    public static List<Method> filterMethodsByParameterCount(List<Method> methods, int parameterCount) {
        List<Method> methodResults = new ArrayList<>();
        for (Method method : methods) {
            if (method.getParameterCount() == parameterCount) {
                methodResults.add(method);
            }
        }
        return methodResults;
    }

    /**
     * Determines whether the specified method name is present in the
     * method-to-class mapping.
     *
     * @param method the name of the method to check for legitimacy
     * @return {@code true} if the method exists in the mapping;
     * {@code false} otherwise
     */
    public static boolean isLegalMethod(String method) {
        return METHOD_CLASS_HASH.containsKey(method);
    }

    /**
     * Retrieves all public methods, including inherited methods, from
     * the specified class. Returns a clone of the array of {@code Method}
     * objects representing all public methods of the class.
     *
     * @param clazz the {@code Class} object from which the public methods
     *             are to be retrieved must not be null
     * @return an array of {@code Method} objects representing all public
     * methods of the specified class, including inherited ones
     */
    private static Method[] getAllCommandMethods(Class<?> clazz) {
        return clazz.getMethods().clone();
    }

    /**
     * Validates whether the signature of an annotation includes a {@code @NockerMethod}
     * matching the provided command method name. This validation ensures that
     * the natural name of the annotated method is not a factor, focusing solely
     * on the signature defined by the {@code @NockerMethod}.
     *
     * @param annotation the {@code Annotation} to inspect. It is checked for the
     *                   presence of a {@code @NockerMethod}.
     * @param commandMethod the name of the command method to compare against the
     *                      {@code @NockerMethod}'s defined name.
     * @return {@code true} if the {@code @NockerMethod} within the annotation matches
     *         the provided {@code commandMethod}, {@code false} otherwise.
     */
    protected static boolean containsNockerMethod(Annotation annotation, String commandMethod) {
        Annotation[] ats = annotation.annotationType().getAnnotations();
        NockerMethod method = null;
        for (Annotation at : ats) {
            if (at.annotationType().equals(NockerMethod.class)) {
                method = (NockerMethod) at;
            }
        }
        if (method != null) {
            String methodName = method.name();
            return methodName.equals(commandMethod);
        }
        return false;
    }

    /**
     * Retrieves a list of methods from the specified class that match the given
     * method name. This method searches through all public methods
     * (including inherited ones) of the class and filters those whose names
     * match the normalized version of the given method name.
     *
     * @param clazz the {@code Class} object from which the methods are to
     *             be retrieved. This class must not be {@code null}.
     * @param methodName a {@code String} representing the name of the
     *                   method to match. The method name is normalized
     *                   for comparison.
     * @return a {@code List<Method>} containing all methods from the specified
     * class that match the given method name. If no matching methods are found,
     * an empty list is returned.
     */
    public static List<Method> getAllMethodFromClass(Class<?> clazz, String methodName) {
        Method[] methods = getAllCommandMethods(clazz);
        List<Method> results = new ArrayList<>();
        for (Method method : methods) {
            try {
                if (method.getName().equals(normalizeClassMethodName(methodName))) {
                    results.add(method);
                }
            } catch (InvalidCommandException e) {
                throw new InvalidCommandException(e.getMessage());
            }
        }
        return results;
    }

    /**
     * Finds and retrieves the {@code Class} associated with a given command
     * method name. This method normalizes the input method name and retrieves
     * the corresponding {@code Class} from a predefined mapping.
     *
     * @param methodName the name of the command method as a {@code String};
     *                  it will be normalized for the lookup process.
     * @return the {@code Class} associated with the normalized method name
     * if found; otherwise returns {@code null}.
     */
    public static Class findClassFromCommandMethodName(String methodName) {
        try {
            methodName = normalizeClassMethodName(methodName);
        } catch (InvalidCommandException e) {
            throw new InvalidCommandException(e.getMessage());
        }
        return METHOD_CLASS_HASH.getOrDefault(methodName, null);
    }

    /**
     * Extracts the parameter names of a given method based on annotations.
     * This method scans all parameters of the provided {@code Method} object
     * and checks if they are annotated with an annotation that is
     * meta-annotated with {@link NockerArg}. If such an annotation is present,
     * its "name" value is extracted and added to the resulting set of
     * parameter names.
     *
     * @param method the {@code Method} object whose parameter names are
     *               to be extracted
     * @return a {@code Set<String>} containing the parameter names extracted
     * from the method, as defined by the annotations
     */
    public static Set<String> getNockerParameterNamesFromMethod(Method method) {
        Parameter[] parameters = method.getParameters();
        Set<String> parameterSet = new HashSet<>();
        for (Parameter parameter : parameters) {
            for (Annotation at : parameter.getAnnotations()) {
                Class<? extends Annotation> atype = at.annotationType();
                if (atype.isAnnotationPresent(NockerArg.class)) {
                    try {
                        String name = (String) atype
                                .getMethod("name")
                                .invoke(at);
                        parameterSet.add(name);
                    } catch (Exception e) {
                        // log something useful
                    }
                }
            }
        }
        return parameterSet;
    }

    /**
     * Normalizes a given method name by transforming it into a camelCase format
     * if it contains a single dash. If no dash is present, the method name is
     * returned as is. When a dash is present, the portion after the dash is
     * capitalized and concatenated with the portion before the dash. If the
     * method name is incorrectly formatted with multiple dashes, an
     * InvalidCommandException is thrown.
     * <p>
     * Note: Applies to class methods only. Does not apply to commandMethods
     * supplied as input.
     *
     * @param commandMethod the original method name to be normalized, which may
     *                      include a single dash as a separator
     * @return the normalized method name, converted to camelCase if applicable
     * @throws InvalidCommandException if the input contains an invalid format with
     * multiple dashes
     */
    private static String normalizeClassMethodName(String commandMethod) throws InvalidCommandException {
        if (commandMethod.contains(SINGLE_DASH)) {
            String[] splitName = commandMethod.split(SINGLE_DASH);
            if (splitName.length != 2) {
                throw new InvalidCommandException("Invalid multipart command method: [" + commandMethod + "]");
            }
            String part1 = splitName[0];
            String part2 = splitName[1];
            return part1 + part2.substring(0, 1).toUpperCase() + part2.substring(1);
        }
        return commandMethod;
    }
}
