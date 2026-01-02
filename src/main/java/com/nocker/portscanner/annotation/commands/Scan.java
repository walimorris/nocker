package com.nocker.portscanner.annotation.commands;

import com.nocker.annotations.NockerMethod;
import com.nocker.command.CommandEngine;
import com.nocker.annotations.NockerArg;

import java.lang.annotation.*;

/**
 * Base scan command. Note that sub commands should contain the base command.
 * This helps the {@link CommandEngine} resolve
 * methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@NockerArg
@NockerMethod(name = "scan")
public @interface Scan {
}
