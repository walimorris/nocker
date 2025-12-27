package com.nocker;

import com.nocker.cli.NockerCommandLineInterface;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("> ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            System.exit(1);
        }
        String[] cliArgs = input.split(" ");
        int exitCode = NockerCommandLineInterface.run(cliArgs);
        System.exit(exitCode);
    }
}