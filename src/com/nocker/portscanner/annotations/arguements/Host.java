package com.nocker.portscanner.annotations.arguements;

import com.nocker.annotations.NockerArg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@NockerArg
public @interface Host {
    String name() default "host";
    boolean required() default true;
}
