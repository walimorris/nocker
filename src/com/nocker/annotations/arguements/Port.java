package com.nocker.annotations.arguements;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@NockerArg
public @interface Port {
    String name() default "port";
    boolean required() default true;
}
