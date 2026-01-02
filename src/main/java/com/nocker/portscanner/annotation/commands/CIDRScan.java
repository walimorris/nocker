package com.nocker.portscanner.annotation.commands;

import com.nocker.annotations.NockerMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@NockerMethod(name = "cidr-scan")
public @interface CIDRScan {
}
