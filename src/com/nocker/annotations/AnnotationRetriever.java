package com.nocker.annotations;

import com.nocker.CIDRWildcard;
import com.nocker.portscanner.PortScanner;
import com.nocker.CommandLineInput;
import com.nocker.InvocationCommand;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Order:
 * 1. get all methods with command
 * 2. get all methods with param count
 * 3. get all methods with exact params
 * 4. maybe we should use something like @primary annotation for tiebreakers
 */
public class AnnotationRetriever {

    private AnnotationRetriever() {
        // static
    }

    // retrieve the method
    public static InvocationCommand retrieve(CommandLineInput commandLineInput) {
        String command = commandLineInput.getCommand();
        Map<String, String> arguments = commandLineInput.getArguments();

        // filter 1: filter methods with command
        List<Method> methodsFromCommand = filterMethodsFromCommand(command);
        // filter 2: filter from param count
        List<Method> methodsFromParamCount = filterMethodsByParameterCount(methodsFromCommand, arguments.size());
        // filter 3: filter by exact param
        return filterMethodsByExactParam(commandLineInput, methodsFromParamCount, arguments);
    }

    protected static InvocationCommand filterMethodsByExactParam(CommandLineInput commandLineInput, List<Method> methods,
                                                                 Map<String, String> arguments) {
        Method winningMethod = null;
        Map<String, Class> parameters = null;
        Set<String> args = new HashSet<>(arguments.keySet());
        for (Method method : methods) {
            Map<String, Class> currentParameters = getParameterNamesAndTypes(method);
            if (currentParameters.keySet().containsAll(args)) {
                winningMethod = method;
                parameters = currentParameters;
                break;
            }
        }
        if (winningMethod == null) {
            return null;
        }
        Object[] commandArgs = convertToObjectArray(parameters, arguments);
        return new InvocationCommand(commandLineInput, winningMethod, commandArgs);
    }

    private static Map<String, Class> getParameterNamesAndTypes(Method method) {
        Map<String, Class> parameters = new LinkedHashMap<>();
        for (Parameter parameter : method.getParameters()) {
            for (Annotation annotation : parameter.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (annotationType.isAnnotationPresent(NockerArg.class)) {
                    try {
                        String name = (String) annotationType.getMethod("name").invoke(annotation);
                        Class<?> type = parameter.getType();
                        parameters.put(name, type);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to read name from annotation", e);
                    }
                }
            }
        }
        return parameters;
    }

    protected static List<Method> filterMethodsByParameterCount(List<Method> methods, int parameterCount) {
        List<Method> methodResults = new ArrayList<>();
        for (Method method : methods) {
            if (method.getParameterCount() == parameterCount) {
                methodResults.add(method);
            }
        }
        return methodResults;
    }

    protected static List<Method> filterMethodsFromCommand(String command) {
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

    private static Method[] getAllCommandMethods() {
        Class<PortScanner> commandClass = PortScanner.class;
        return commandClass.getMethods();
    }

    private static Object[] convertToObjectArray(Map<String, Class> parameters, Map<String, String> args) {
        return parameters.entrySet()
                .stream()
                .map(entry -> convert(args.get(entry.getKey()), entry.getValue()))
                .toArray(Object[]::new);
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
        if (type == CIDRWildcard.class) {
            return new CIDRWildcard(value);
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    // probably could be a bit more robust
    private static boolean isListCommaDelimited(String str) {
        return str.trim().contains(",");
    }
}
