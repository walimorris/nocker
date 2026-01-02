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
    private final CommandMethod commandMethod;
    private final LinkedHashMap<String, String> arguments;
    private final LinkedHashMap<String, String> flags;

    public static final String ARG_SEPARATOR = "=";
    public static final String SINGLE_DASH = "-";
    public static final String DOUBLE_DASH = "--";
    public static final String EMPTY_SPACE = "\\s";

    // move to enum
    private static final Set<String> legalMethods = new HashSet<>(Arrays.asList(
            "scan",
            "cidr-scan"
    ));

    private CommandLineInput(String command, CommandMethod commandMethod, LinkedHashMap<String, String> args,
            LinkedHashMap<String, String> flags) {
        this.command = command;
        this.commandMethod = commandMethod;
        this.arguments = args;
        this.flags = flags;
    }

    public static CommandLineInput parse(String[] args) {
        ParsedInput parsedInput = validateAndParse(args);
        return new CommandLineInput(
                parsedInput.getCommand(),
                parsedInput.getCommandMethod(),
                new LinkedHashMap<>(parsedInput.getArguments()),
                new LinkedHashMap<>(parsedInput.getFlags()));
    }

    public String getCommand() {
        return this.command;
    }

    public CommandMethod getCommandMethod() {
        return this.commandMethod;
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
        String command = String.join(EMPTY_SPACE, args);
        Map<Class<?>, LinkedHashMap<String, String>> hash = parseArgumentsAndFlags(args);
        CommandMethod commandMethod = validateFromMethodSignature(args, hash.get(Arg.class));
        return new ParsedInput(command, commandMethod, Collections.unmodifiableMap(new LinkedHashMap<>(hash.get(Arg.class))),
                Collections.unmodifiableMap(new LinkedHashMap<>(hash.get(Flag.class))));
    }

    /**
     * This method is provided the origin command input and a {@link LinkedHashMap} of the
     * validated command arguments.The initial command passed to {@code nocker} is parsed
     * and utilizes the {@link MethodResolver} to validate that the method supplied after
     * the {@code nocker} namespace is a valid option. If so, all methods from that class
     * are read and matched against the qualified and validated command arguments. if a
     * match is not found, a {@link InvalidCommandException} is thrown because there was no
     * suitable match for the given arguments.
     * <p>
     * <b>Rule</b>: The order of the supplied arguments do not matter. Arguments in any order
     * is completely legal, {@code nocker} will parse and respond accordingly. What matters is
     * that an argument contains a valid inline value.
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
    private static CommandMethod validateFromMethodSignature(String[] commandInputs, LinkedHashMap<String, String> arguments) {
        String commandMethodName = commandInputs[1];
        Class<?> clazz = MethodResolver.findClassFromCommandMethodName(commandMethodName);
        if (clazz == null) {
            throw new InvalidCommandException("Illegal command method: " + commandMethodName);
        }
        List<Method> possibleMethods = MethodResolver.filterMethodsFromCommand(commandMethodName, clazz);
        boolean matchFound = false;
        for (Method method : possibleMethods) {
            Set<String> params = MethodResolver.getNockerParameterNamesFromMethod(method);
            Set<String> currentCommandArguments = arguments.keySet();
            if (params.equals(currentCommandArguments)) {
                return new CommandMethod(commandMethodName, method.getName(), method);
            }
        }
        /**
         * Be aware, Nocker annotation engine supplied default names for valid arguments. Changing
         * the default name will change outcome. However, the annotation engine does not care about
         * the name of the supplied parameter, it was always take the name of the NockerArg.
         */
        if (!matchFound) {
            throw new InvalidCommandException("The supplied arguments are invalid: " + arguments.keySet());
        }
        return null;
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

    /**
     * Parses a command-line argument token and maps it to its corresponding value in
     * the provided token hashmap.  If the token is not properly formatted or invalid,
     * an exception is thrown.
     *
     * <p>
     * <b>Rules</b>: arguments must contain two forward dashes, and a key value pair,
     * an '=' sign is inline and separates the key/value.
     * <pre>
     * {@code
     * --host=127.0.0.1
     * }
     * </pre>
     *
     * @param token the command-line argument token to be parsed.
     * @param tokenHash a map where the parsed key-value pair from the token will be
     *                  stored. The key is derived from the argument name, and the
     *                  value corresponds to the parsed value from the token.
     * @throws InvalidCommandException if the token does not include a valid argument
     * format or cannot be resolved.
     */
    private static void getParsedArgument(String token, LinkedHashMap<String, String> tokenHash) {
        if (containsEquals(token)) {
            String currentTokenArgument = token.split(ARG_SEPARATOR, 2)[0];
            String currentTokenValue = token.split(ARG_SEPARATOR, 2)[1];
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

    /**
     * Parses a command-line flag token, determines if it is valid, and maps it to its
     * corresponding value in the provided token hash. If the flag format is invalid,
     * an exception is thrown.
     * <p>
     * <pre>
     * {@code
     * legal: -o myFiles/results.json -> next token, the file for storage, is consumed
     * legal: -out=myFiles/results.json -> next token is not consumed
     * }
     * </pre>
     *
     * @param token the flag token to be processed
     * @param nextToken the next token in the sequence, used as the value if the current
     *                 token does not include an inline value
     * @param tokenHash a map where the parsed flag name and its associated value will be
     *                  stored. The key is the flag's full name, and the value is the parsed
     *                  value.
     * @return true if the next token was consumed as the flag's value, false otherwise
     * @throws InvalidCommandException if the flag token is invalid or cannot be resolved
     */
     private static boolean getParsedFlag(String token, String nextToken, LinkedHashMap<String, String> tokenHash) {
        String tokenValue = containsEquals(token) ? token.split(ARG_SEPARATOR, 2)[1] : nextToken;
        FlagValue flag = fromFlagToken(token, tokenValue);
        if (flag != null) {
            // a flag's fullname is its final state
            tokenHash.put(flag.getFlag().getFullName(), flag.getValue());
            return !containsEquals(token);
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

    /**
     * Normalizes a command-line flag token by removing preceding decorations, and
     * stripping optional argument separators if present. Validates that the token
     * starts with a single dash (-) and contains valid characters.
     * <p>
     * <p>
     * <pre>
     * {@code
     * legal: -timeout=100 -> timeout
     * legal: -t 100 -> t
     * legal: -t=100 -> t
     *
     * illegal: t=100
     * illegal: timeout 100
     * }
     * </pre>
     * @param token the raw flag token to be normalized
     * @return the normalized flag token
     * @throws InvalidCommandException if the token is not legal flag format
     */
    private static String normalizeFlagToken(String token) {
        token = token.trim();
        if (!isValidFlagStart(token)) {
            throw new InvalidCommandException("Illegal flag: " + token);
        }
        token = token.substring(1);
        token = containsEquals(token) ? token.split(ARG_SEPARATOR, 2)[0] : token;
        return token.trim();
    }

    /**
     * Normalizes a command-line argument token by removing any prepended decorations
     * such as double dashes (--) and an argument separator (=). It processes tokens
     * that start with a valid argument format:
     * <p>
     * <pre>
     * {@code
     * legal: --hosts=127.0.0.1 -> hosts
     *
     * illegal: hosts=127.0.0.1
     * illegal: --hosts 127.0.0.1
     * }
     * </pre>
     *
     * @param token the raw argument token to be normalized.
     * @return the normalized argument key extracted from the token
     * @throws InvalidCommandException if the token is not a valid argument format
     */
    private static String normalizeArgToken(String token) {
        token = token.trim();
        if (!isValidArgStart(token)) {
            throw new InvalidCommandException("Illegal argument: " + token);
        }
        String fullyQualifiedArgument = token.substring(2);
        return fullyQualifiedArgument.split(ARG_SEPARATOR, 2)[0];
    }

    private static boolean isValidFlagStart(String token) {
        return startsWithSingleDash(token) && isValidLetterCharacter(token.charAt(1));
    }

    private static boolean isValidArgStart(String token) {
        return startsWithDoubleDash(token)
                && isValidLetterCharacter(token.charAt(2))
                && containsEquals(token);
    }

    private static boolean isValidLetterCharacter(char c) {
        return Character.isLetter(c);
    }

    private static boolean containsEquals(String token)  {
        return token.contains(ARG_SEPARATOR);
    }

    private static boolean startsWithSingleDash(String token) {
        return token.startsWith(SINGLE_DASH);
    }

    private static boolean startsWithDoubleDash(String token) {
        return token.startsWith(DOUBLE_DASH);
    }

    private static final class ParsedInput {
        private final String command;
        private final CommandMethod commandMethod;
        private final Map<String, String> arguments;
        private final Map<String, String> flags;

        private ParsedInput(String command, CommandMethod commandMethod, Map<String, String> arguments,
                            Map<String, String> flags) {
            this.command = command;
            this.commandMethod = commandMethod;
            this.arguments = arguments;
            this.flags = flags;
        }

        String getCommand() {
            return this.command;
        }

        CommandMethod getCommandMethod() {
            return this.commandMethod;
        }

        Map<String, String> getArguments() {
            return this.arguments;
        }

        Map<String, String> getFlags() {
            return this.flags;
        }
    }
}
