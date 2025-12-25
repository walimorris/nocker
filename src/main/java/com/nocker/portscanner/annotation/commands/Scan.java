package com.nocker.portscanner.annotation.commands;

import com.nocker.annotations.AnnotationRetriever;
import com.nocker.annotations.CommandType;
import com.nocker.annotations.NockerArg;

import java.lang.annotation.*;

/**
 * Base scan command. Note that sub commands should contain the base command.
 * This helps the {@link AnnotationRetriever} resolve
 * methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@NockerArg
@CommandType(name = "scan")
public @interface Scan {
}
