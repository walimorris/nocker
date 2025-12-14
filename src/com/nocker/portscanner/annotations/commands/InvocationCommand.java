package com.nocker.portscanner.annotations.commands;

import java.lang.reflect.Method;

public record InvocationCommand(CommandLineInput commandLineInput, Method method, Object[] args) {
}
