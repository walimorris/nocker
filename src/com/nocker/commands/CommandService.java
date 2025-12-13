package com.nocker.commands;

import com.nocker.annotations.arguements.Host;
import com.nocker.annotations.arguements.Port;
import com.nocker.annotations.commands.Scan;

public class CommandService {

    @Scan
    public void scanPort(@Host String host, @Port int port) {

    }

    @Scan
    public void scanPort(@Port int port) {

    }
}
