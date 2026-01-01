package com.nocker.command;

import com.nocker.annotations.CommandType;
import com.nocker.annotations.NockerArg;
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
    public static LinkedHashMap<String, Class> getParameterNamesAndTypes(Method method) {
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

    public static List<Method> filterMethodsFromCommand(String command, Class clazz) {
        Method[] methods = getAllCommandMethods(clazz);
        List<Method> possibleMethods = new ArrayList<>();
        for (Method method : methods) {
            Annotation[] declaredAnnotations = method.getDeclaredAnnotations();
            if (CollectionUtils.isNotEmpty(Arrays.asList(declaredAnnotations))) {
                // need to force single annotation on method
                Annotation methodAnnotation = declaredAnnotations[0];
                if (containsCommandType(methodAnnotation, command)) {
                    possibleMethods.add(method);
                }
            }
        }
        return possibleMethods;
    }

    public static List<Method> filterMethodsByParameterCount(List<Method> methods, int parameterCount) {
        List<Method> methodResults = new ArrayList<>();
        for (Method method : methods) {
            if (method.getParameterCount() == parameterCount) {
                methodResults.add(method);
            }
        }
        return methodResults;
    }

    public static boolean isLegalMethod(String method) {
        return METHOD_CLASS_HASH.containsKey(method);
    }

    private static Method[] getAllCommandMethods(Class<?> clazz) {
        return clazz.getMethods();
    }

    private static boolean containsCommandType(Annotation annotation, String commandTypeStr) {
        Annotation[] ats = annotation.annotationType().getAnnotations();
        CommandType commandType = null;
        for (Annotation at : ats) {
            if (at.annotationType().equals(CommandType.class)) {
                commandType = (CommandType) at;
            }
        }
        if (commandType != null) {
            return commandType.name().equals(commandTypeStr);
        }
        return false;
    }

    public static List<Method> getAllMethodFromClass(Class<?> clazz, String name) {
        Method[] methods = getAllCommandMethods(clazz);
        List<Method> results = new ArrayList<>();
        for (Method method : methods) {
            if (method.getName().equals(normalizeMethodName(name))) {
                results.add(method);
            }
        }
        return results;
    }

    public static Class findClassFromMethodName(String methodName) {
        methodName = normalizeMethodName(methodName);
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
    public static Set<String> getParameterNamesFromMethod(Method method) {
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

    private static String normalizeMethodName(String commandMethod) {
        if (commandMethod.contains("-")) {
            String[] splitName = commandMethod.split(SINGLE_DASH, 2);
            if (splitName.length > 2) {
                throw new InvalidCommandException("command method with three parts not supported.");
            }
            String part1 = splitName[0];
            String part2 = splitName[1];
            return part1 + part2.substring(0, 1).toUpperCase() + part2.substring(1);
        }
        return commandMethod;
    }
}
