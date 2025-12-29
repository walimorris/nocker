package com.nocker.command;

import com.nocker.annotations.CommandType;
import com.nocker.annotations.NockerArg;
import com.nocker.portscanner.PortScanner;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

final class MethodResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodResolver.class);

    private MethodResolver() {
        throw new AssertionError("Cannot instantiate instance of final class" + getClass().getName());
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
     * @param method the {@code Method} object whose parameters' names and types are to be extracted
     * @return a {@code LinkedHashMap} where the keys are parameter names and the values are the corresponding parameter types,
     *         preserving the declaration order of the method's parameters
     * @throws RuntimeException if an error occurs during annotation parsing or reflection calls
     */
    public static LinkedHashMap<String, Class> getParameterNamesAndTypes(Method method) {
        LinkedHashMap<String, Class> parameters = new LinkedHashMap<>();
        for (Parameter parameter : method.getParameters()) {
            for (Annotation annotation : parameter.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (annotationType.isAnnotationPresent(NockerArg.class)) {
                    try {
                        String name = (String) annotationType.getMethod("name").invoke(annotation);
                        Class<?> type = parameter.getType();
                        parameters.put(name, type);
                    } catch (Exception e) {
                        LOGGER.error("Error parsing annotation, parameter name and type from given method: [{}#{}] with parameters {}: {}",
                                method.getClass().getName(), method.getName(),
                                method.getParameters(), e.getStackTrace());
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return parameters;
    }

    public static List<Method> filterMethodsFromCommand(String command) {
        Method[] methods = getAllCommandMethods();
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

    private static Method[] getAllCommandMethods() {
        Class<PortScanner> commandClass = PortScanner.class;
        return commandClass.getMethods();
    }

    private static boolean containsCommandType(Annotation annotation, String commandTypeStr) {
        Annotation[] annotations = annotation.annotationType().getAnnotations();
        CommandType commandType = null;
        for (Annotation a : annotations) {
            if (a.annotationType().equals(CommandType.class)) {
                commandType = (CommandType) a;
            }
        }
        if (commandType != null) {
            return commandType.name().equals(commandTypeStr);
        }
        return false;
    }
}
