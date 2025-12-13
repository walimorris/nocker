package com.nocker.commands;

import java.lang.reflect.Method;

public record InvocationCommand(Method method, Object[] args) {
}
