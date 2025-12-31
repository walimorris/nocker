package com.nocker.portscanner.command;

import com.nocker.Arg;
import com.nocker.Flag;
import com.nocker.command.MethodResolver;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

public final class CommandLineInput {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineInput.class);

    private final String command;
    private final LinkedHashMap<String, String> arguments;
    private final LinkedHashMap<String, String> flags;

    // move to enum
    private static final Set<String> legalMethods = new HashSet<>(Arrays.asList(
            "scan",
            "cidr-scan"
    ));

    private CommandLineInput(String command, LinkedHashMap<String, String> args,
            LinkedHashMap<String, String> flags) {
        this.command = command;
        this.arguments = args;
        this.flags = flags;
    }

    public static CommandLineInput parse(String[] args) {
        ParsedInput parsedInput = validateAndParse(args);
        return new CommandLineInput(
                parsedInput.getCommand(),
                new LinkedHashMap<>(parsedInput.getArguments()),
                new LinkedHashMap<>(parsedInput.getFlags()));
    }

    public String getCommand() {
        return this.command;
    }

    public LinkedHashMap<String, String> getArguments() {
        return this.arguments;
    }

    public LinkedHashMap<String, String> getFlags() {
        return this.flags;
    }

    @Override
    public String toString() {
        return null;
    }

    private static ParsedInput validateAndParse(String[] args) {
        // a minimal command: nocker scan --host=localhost
        if (args == null) {
            throw new InvalidCommandException("Cannot parse empty command");
        }
        if (!args[0].equals("nocker")) {
            throw new InvalidCommandException("Illegal: are you trying to break me?!");
        }
        if (!MethodResolver.isLegalMethod(args[1])) {
            throw new InvalidCommandException("Illegal method. Possible options are: \n" + legalMethods);
        }
        String command = String.join(" ", args);
        Map<Class<?>, LinkedHashMap<String, String>> hash = parseArgumentsAndFlags(args);
        validateFromMethodSignature(args, hash.get(Arg.class));
        return new ParsedInput(command, Collections.unmodifiableMap(new LinkedHashMap<>(hash.get(Arg.class))),
                Collections.unmodifiableMap(new LinkedHashMap<>(hash.get(Flag.class))));
    }

    /**
     * This method is provided the origin command input and a {@link LinkedHashMap} of the validated command arguments.
     * The initial command passed to {@code nocker} is parsed and utilizes the {@link MethodResolver} to validate that
     * the method supplied after the {@code nocker} namespace is a valid option. If so, all methods from that class are
     * read and matched against the qualified and validated command arguments. if a match is not found, a
     * {@link InvalidCommandException} is thrown because there was no suitable match for the given arguments.
     * <p>
     * <b>Rule</b>: The order of the supplied arguments do not matter. Arguments in any order is completely legal, {@code nocker}
     * will parse and respond accordingly. What matters is that an argument contains a valid inline value.
     *
     * <p>
     * Example: the following arguments are equivalent
     * </p>
     * <pre>
     * {@code
     * nocker scan --host=127.0.0.1 --port=8080
     * nocker scan --port=8080 --host=127.0.0.1
     * }
     * </pre>
     * @param commandInputs the original supplied command
     * @param arguments parsed arguments from validation
     */
    private static void validateFromMethodSignature(String[] commandInputs, LinkedHashMap<String, String> arguments) {
        String commandMethodName = commandInputs[1];
        Class<?> clazz = MethodResolver.findClassFromMethodName(commandMethodName);
        if (clazz == null) {
            throw new InvalidCommandException("Illegal command method: " + commandMethodName);
        }
        List<Method> possibleMethods = MethodResolver.getAllMethodFromClass(clazz, commandMethodName);
        boolean matchFound = false;
        for (Method method : possibleMethods) {
            Set<String> params = MethodResolver.getParameterNamesFromMethod(method);
            Set<String> currentCommandArguments = arguments.keySet();
            if (params.equals(currentCommandArguments)) {
                matchFound = true;
                break;
            }
        }
        if (!matchFound) {
            throw new InvalidCommandException("The supplied arguments are invalid: " + arguments.keySet());
        }
    }

    private static Map<Class<?>, LinkedHashMap<String, String>> parseArgumentsAndFlags(String[] args) {
        Map<Class<?>, LinkedHashMap<String, String>> argAndFlagHash = new HashMap<>();
        LinkedHashMap<String, String> argumentsHash = new LinkedHashMap<>();
        LinkedHashMap<String, String> flagsHash = new LinkedHashMap<>();
        List<String> shortenedInput = shortenInput(args);
        if (CollectionUtils.isNotEmpty(shortenedInput)) {
            for (int i = 0; i < shortenedInput.size(); i++) {
                String currentToken = shortenedInput.get(i);
                if (isValidArgStart(currentToken)) {
                    getParsedArgument(currentToken, argumentsHash);
                    continue;
                }
                if (isValidFlagStart(currentToken)) {
                    // ensure there's a next token if the flag does not have an inline value(i.e., -timeout=100)
                    if (!containsEquals(currentToken) && i + 1 >= shortenedInput.size()) {
                        throw new InvalidCommandException(
                                "Flag requires a value: " + currentToken
                        );
                    }
                    // pass the next token if this flag is not inline (i.e., looks like so: -t 100)
                    String nextToken = containsEquals(currentToken) ? null : shortenedInput.get(i + 1);
                    boolean consumedNextToken = getParsedFlag(currentToken, nextToken, flagsHash);
                    if (consumedNextToken) {
                        i++;
                    }
                } else {
                    throw new InvalidCommandException("This is not a legal command: " + currentToken);
                }
            }
        }
        argAndFlagHash.put(Arg.class, argumentsHash);
        argAndFlagHash.put(Flag.class, flagsHash);
        return argAndFlagHash;
    }

    // check arguments are not null
    // fully valid arguments in nocker contains '='
    // example 'nocker scan --host=localhost --port=8080 -ax'
    // -ax is a special argument with enhancements on the
    // fully qualified commandLine input
    private static void getParsedArgument(String token, LinkedHashMap<String, String> tokenHash) {
        if (containsEquals(token)) {
            String currentTokenArgument = token.split("=")[0];
            String currentTokenValue = token.split("=")[1];
            ArgumentValue arg = fromArgToken(token, currentTokenValue);
            if (arg != null) {
                tokenHash.put(arg.getArgument().getArgumentName(), arg.getValue());
            } else {
                throw new InvalidCommandException("This is a illegal argument[" + currentTokenArgument + "]");
            }
        } else {
            throw new InvalidCommandException("This is a illegal argument[" + token + "]. " +
                    "Example of valid argument[--host=127.0.0.1].");
        }
    }

     // works, but can be optimized by skipping the next token if
     // the previous token was an abbreviation ex: -t 5000
     private static boolean getParsedFlag(String token, String nextToken, LinkedHashMap<String, String> tokenHash) {
        String tokenValue = containsEquals(token) ? token.split("=", 2)[1] : nextToken;
        FlagValue flag = fromFlagToken(token, tokenValue);
        if (flag != null) {
            // a flag's fullname is its final state
            tokenHash.put(flag.getFlag().getFullName(), flag.getValue());
            return !containsEquals(token); // was consumed?
        }
        throw new InvalidCommandException("Illegal flag: " + token);
    }

    private static List<String> shortenInput(String[] inputCommand) {
        List<String> input = Arrays.asList(inputCommand);
        if (CollectionUtils.isNotEmpty(input)) {
            // ignore namespace and command
            return input.subList(2, input.size());
        }
        return null;
    }

    private static FlagValue fromFlagToken(String token, String tokenValue) {
        String normalizedToken = normalizeFlagToken(token);
        Optional<Flag> flag = Arrays.stream(Flag.values())
                // flags can be supplied in both forms so this response with the given form its first found in
                .filter(f -> f.getAbbreviatedName().equals(normalizedToken) || f.getFullName().equals(normalizedToken))
                .findFirst();
        return flag.map(value -> new FlagValue(value, tokenValue)).orElse(null);
    }

    private static ArgumentValue fromArgToken(String token, String tokenValue) {
        String normalizedToken = normalizeArgToken(token);
        Optional<Arg> arg = Arrays.stream(Arg.values())
                .filter(f -> f.getArgumentName().equals(normalizedToken))
                .findFirst();
        return arg.map(value -> new ArgumentValue(value, tokenValue)).orElse(null);
    }

    private static String normalizeFlagToken(String token) {
        token = token.trim();
        if (isValidFlagStart(token)) {
            token = token.substring(1);
            token = containsEquals(token) ? token.split("=")[0] : token;
            return token.trim();
        }
        return null;
    }

    // normalized arg token strip the prepended decorations (--, =)
    // nocker scan --host=localhost --port=8080
    private static String normalizeArgToken(String token) {
        token = token.trim();
        if (isValidArgStart(token)) {
            String fullyQualifiedArgument = token.substring(2);
            return fullyQualifiedArgument.split("=")[0];
        }
        return null;
    }

    public static boolean isValidFlagStart(String token) {
        return startsWithSingleDash(token) && isValidLetterCharacter(token.charAt(1));
    }

    public static boolean isValidArgStart(String token) {
        return startsWithDoubleDash(token)
                && isValidLetterCharacter(token.charAt(2))
                && containsEquals(token);
    }
    public static boolean isValidLetterCharacter(char c) {
        return Character.isLetter(c);
    }

    public static boolean containsEquals(String token)  {
        return token.contains("=");
    }

    public static boolean startsWithSingleDash(String token) {
        return token.startsWith("-");
    }

    public static boolean startsWithDoubleDash(String token) {
        return token.startsWith("--");
    }

    private static final class ParsedInput {
        private final String command;
        private final Map<String, String> arguments;
        private final Map<String, String> flags;

        private ParsedInput(String command, Map<String, String> arguments,
                            Map<String, String> flags) {
            this.command = command;
            this.arguments = arguments;
            this.flags = flags;
        }

        String getCommand() {
            return this.command;
        }

        Map<String, String> getArguments() {
            return this.arguments;
        }

        Map<String, String> getFlags() {
            return this.flags;
        }
    }
}
