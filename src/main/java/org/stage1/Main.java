package org.stage1;

import java.util.Scanner;

/**
 * Objectives
 * In this stage, your program should:
 *
 * Demonstrate that it is running by printing its title: Learning Progress Tracker.
 *
 * Wait for the commands. In this stage, the only command the program should recognize is exit. Once a user enters it, the program should print Bye! and quit.
 *
 * Detect if a user has entered a blank line and print No input in response.
 *
 * Print Unknown command! if a user enters an unknown command.
 */
public class Main {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Learning Progress Tracker");

        while (true) {
            String input = SCANNER.nextLine().trim();

            if(input.equals("exit")) {
                System.out.println("Bye!");
                break;
            }

            if(input.isEmpty() || input.isBlank()) {
                System.out.println("No input.");
            } else {
                System.out.println("Unknown command!");
            }
        }
    }
}
