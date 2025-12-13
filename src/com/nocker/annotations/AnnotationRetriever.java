package com.nocker.annotations;

import com.nocker.annotations.arguements.NockerArg;
import com.nocker.annotations.commands.CommandType;
import com.nocker.commands.CommandService;
import com.nocker.commands.CommandLineInput;
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
    public static Method retrieve(CommandLineInput commandLineInput) {
        String command = commandLineInput.getCommand();
        List<String> arguments = commandLineInput.getArguments();

        // filter 1: filter methods with command
        List<Method> methodsFromCommand = filterMethodsFromCommand(command);
        // filter 2: filter from param count
        List<Method> methodsFromParamCount = filterMethodsByParameterCount(methodsFromCommand, arguments.size());
        // filter 3: filter by exact param
        return filterMethodsByExactParam(methodsFromParamCount, arguments);
    }

    protected static Method filterMethodsByExactParam(List<Method> methods, List<String> arguments) {
        // later we should add a tiebreaker annotation such as @Primary in case we have multiple
        // winning methods due to deprecation. However, this should not be the case because nocker
        // is Command -> argument driven. a Command such as "scan" should not have the same arguments.
        // it can have same argument count, but only way the same argument should be available is in
        // the case of deprecation or command updates
        List<Method> winningMethods = new ArrayList<>();
        Set<String> args = new HashSet<>(arguments);
        for (Method method : methods) {
            Set<String> parameters = getParameterNames(method);
            if (parameters.containsAll(args)) {
                winningMethods.add(method);
            }
        }
        return winningMethods.isEmpty() ? null : winningMethods.get(0);
    }

    private static Set<String> getParameterNames(Method method) {
        Set<String> parameters = new HashSet<>();
        for (Parameter parameter : method.getParameters()) {
            for (Annotation annotation : parameter.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (annotationType.isAnnotationPresent(NockerArg.class)) {
                    try {
                        String name = (String) annotationType.getMethod("name").invoke(annotation);
                        parameters.add(name);
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
        Class<CommandService> commandClass = CommandService.class;
        return commandClass.getMethods();
    }
}
