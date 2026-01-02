package com.nocker.portscanner.annotation.commands;

import com.nocker.annotations.NockerMethod;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@NockerMethod(name = "scan")
public @interface Scan {
}
