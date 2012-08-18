package com.github.tarsa.tarsalzp;

import java.io.Console;

/**
 *
 * @author Piotr Tarsa
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        final Console console = System.console();
        System.out.println(console == null ? "absent" : "present");
    }
}
