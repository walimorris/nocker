package com.nocker.portscanner.annotations.commands;

import com.nocker.annotations.CommandType;
import com.nocker.annotations.NockerArg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@NockerArg
@CommandType(name = "cidr-scan")
public @interface CIDRScan {
}
