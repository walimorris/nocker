package com.nocker.portscanner.annotation.arguments;

import com.nocker.annotations.NockerArg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@NockerArg
public @interface Hosts {
    String name() default "hosts";
    boolean required() default true;
}
