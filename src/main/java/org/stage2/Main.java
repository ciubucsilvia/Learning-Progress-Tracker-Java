package org.stage2;


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Objectives
 * In addition to the features of the first stage, your program should:
 *
 * Recognize a new command: add students and respond with the following message:
 * Enter student credentials or 'back' to return.
 * Recognize a new back command and react as follows: if users want to finish adding
 * new students, the program should print a message with the total number of students
 * added during the session, for example: Total 5 students have been added. Otherwise,
 * print a hint: Enter 'exit' to exit the program.
 * The program should read user credentials from the console and check whether they
 * match the established patterns. If the credentials match all patterns, print
 * The student has been added. Otherwise, it should print which part of
 * the credentials is not acceptable: Incorrect first name, Incorrect last name and
 * Incorrect email.
 * If the input cannot be interpreted as valid credentials, the program should print
 * Incorrect credentials.
 */
public class Main {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final List<String> students = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Learning Progress Tracker");

        while (true) {
            String input = SCANNER.nextLine().trim();

            if (input.equals("exit")) {
                System.out.println("Bye!");
                break;
            }

            if (input.isEmpty() || input.isBlank()) {
                System.out.println("No input.");
                continue;
            }

            if (input.equals("back")) {
                System.out.println("Enter 'exit' to exit the program.");
                continue;
            }

            if (input.equals("add students")) {
                addStudents();
            } else if (input.equals("back")) {
                System.out.printf("Total %d students have been added.\n",
                        students.size());
                break;
            } else {
                System.out.println("Unknown command!");
            }
        }
    }

    public static void addStudents() {
        System.out.println("Enter student credentials or 'back' to return:");
        while (true) {
            String student = SCANNER.nextLine().trim();


            if(student.equals("back")) {
                System.out.printf("Total %d students have been added.\n",
                        students.size());
                break;
            }

            if (student.isEmpty()) {
                System.out.println("Incorrect credentials");
                continue;
            }

            String[] info = student.split(" ");

            if(info.length < 3) {
                System.out.println("Incorrect credentials");
                continue;
            } else {
                String firstName = info[0];

                StringBuilder lastNameBuilder = new StringBuilder();
                for(int i = 1; i < info.length - 1; i++) {
                    if (i > 1) {
                        lastNameBuilder.append(" ");
                    }
                    lastNameBuilder.append(info[i]);
                }

                String lastName = lastNameBuilder.toString();

                String email = info[info.length - 1];

                boolean validFirstName = validateName(firstName);
                boolean validLastName = validateName(lastName);
                boolean validEmail = validateEmail(email);


                if(!validFirstName && !validLastName && !validEmail) {
                    System.out.println("Incorrect credentials");
                } else if(!validFirstName) {
                    System.out.println("Incorrect first name");
                } else if (!validLastName) {
                    System.out.println("Incorrect last name");
                } else if(!validEmail) {
                    System.out.println("Incorrect email");
                } else {
                    students.add(firstName + " "
                            + lastName + " "
                            + email);

                    System.out.println("The student has been added.");
                }
            }
        }
    }

    public static boolean validateName(String name) {
        if (name.length() < 2) {
            return false;
        }

        // Check for valid characters and pattern
        // Name can contain letters (A-Z, a-z), hyphens (-), and apostrophes (')
        // Hyphens and apostrophes cannot be at start or end
        // Hyphens and apostrophes cannot be adjacent to each other
        String regex = "^[a-zA-Z]+([-' ][a-zA-Z]+)*$";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(name);

        return matcher.matches();
    }

    public static boolean validateEmail(String email) {
//        String regex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
//
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(email);
//
//        return matcher.matches();

        if (email == null || email.isEmpty()) {
            return false;
        }

        int atCount = 0;
        for (char c : email.toCharArray()) {
            if (c == '@') {
                atCount++;
            }
        }

        if (atCount != 1) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return false;
        }

        String afterAt = email.substring(atIndex + 1);
        int dotIndex = afterAt.indexOf('.');

        return dotIndex > 0 && dotIndex < afterAt.length() - 1;
    }
}