package com.nocker.annotations.commands;

import com.nocker.annotations.arguements.NockerArg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@NockerArg
@CommandType(name = "scan")
public @interface Scan {
}
