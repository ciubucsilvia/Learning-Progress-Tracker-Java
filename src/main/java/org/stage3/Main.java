package org.stage3;

import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main entry point for the Learning Progress Tracker.
 * Manages the CLI loop and delegates commands to specific methods.
 */
public class Main {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static Integer firstId = 10000; // Starting ID for students

    public static void main(String[] args) {
        // Initialize the database and tables
        createDB();

        System.out.println("Learning Progress Tracker");
        boolean running = true;

        // Main command loop
        while (running) {
            String input = SCANNER.nextLine().trim();

            if (input.isEmpty() || input.isBlank()) {
                System.out.println("No input.");
                continue;
            }

            // Command routing
            switch (input) {
                case "exit" -> {
                    System.out.println("Bye!");
                    running = false;
                }
                case "back" -> breakApp();
                case "add students" -> addStudents();
                case "list" -> getList();
                case "add points" -> addPoints();
                case "find" -> findStudent();
                default -> System.out.println("Unknown command!");
            }
        }
        // Cleanup connection before exit
        ConnectDBSqlite.closeConnection();
    }

    /**
     * Initializes the database by deleting the old one and creating fresh tables.
     */
    public static void createDB(){
        ConnectDBSqlite.deleteDB();

        Student student = new Student();
        student.createTableStudents();

        Course course = new Course();
        course.createTableCourses();
        course.createTableStudentCourse();

        // Populate the courses table with default values
        course.createCourses();
    }

    /**
     * Handles the 'back' command at the top level or shows student count.
     */
    public static void breakApp() {
        Student student = new Student();
        int numberStudents = student.getNumberStudents();
        if(numberStudents > 0) {
            System.out.printf("Total %d students have been added.\n", numberStudents);
        } else {
            System.out.println("Enter 'exit' to exit the program.");
        }
    }

    /**
     * Logic for the 'add students' command.
     * Validates input format, names, and emails before saving to DB.
     */
    public static void addStudents() {
        Student student = new Student();

        System.out.println("Enter student credentials or 'back' to return:");
        while (true) {
            String input = SCANNER.nextLine().trim();
            int numberStudents = student.getNumberStudents();

            if(input.equals("back")) {
                System.out.printf("Total %d students have been added.\n", numberStudents);
                break;
            }

            if (input.isEmpty()) {
                System.out.println("Incorrect credentials");
                continue;
            }

            // Split input by whitespace: [FirstName, LastPart1, LastPart2..., Email]
            String[] info = input.split(" ");

            if(info.length < 3) {
                System.out.println("Incorrect credentials");
                continue;
            }

            String firstName = info[0];

            // Collect everything between first name and email as the last name
            StringBuilder lastNameBuilder = new StringBuilder();
            for(int i = 1; i < info.length - 1; i++) {
                if (i > 1) {
                    lastNameBuilder.append(" ");
                }
                lastNameBuilder.append(info[i]);
            }

            String lastName = lastNameBuilder.toString();
            String email = info[info.length - 1];

            // Run validation checks
            boolean validFirstName = Student.validateName(firstName);
            boolean validLastName = Student.validateName(lastName);
            boolean validEmail = student.validateEmail(email);

            if(!validFirstName && !validLastName && !validEmail) {
                System.out.println("Incorrect credentials");
            } else if(!validFirstName) {
                System.out.println("Incorrect first name");
            } else if (!validLastName) {
                System.out.println("Incorrect last name");
            } else if(!validEmail) {
                System.out.println("Incorrect email");
            } else {
                // Generate a simple ID based on existing count
                int id = firstId + numberStudents;

                student.setId(id);
                student.setFirstName(firstName);
                student.setLastName(lastName);
                student.setEmail(email);

                // Check for unique email constraint in the database
                if(student.isEmailUsed()) {
                    System.out.println("This email is already taken.");
                    continue;
                }

                student.addStudent();
                System.out.println("The student has been added.");
            }
        }
    }

    /**
     * Lists all student IDs currently in the system.
     */
    public static void getList() {
        Student student = new Student();
        List<Integer> ids = student.getAllStudentsId();

        if(ids.size() > 0) {
            System.out.println("Students:");
            for(Integer id: ids) {
                System.out.println(id);
            }
        } else {
            System.out.println("No students found");
        }
    }

    /**
     * Handles the 'add points' command.
     * Format: <Student ID> <Java> <DSA> <Databases> <Spring>
     */
    public static void addPoints() {
        Student student = new Student();
        System.out.println("Enter an id and points or 'back' to return:");

        while (true) {
            String input = SCANNER.nextLine().trim();

            if (input.equals("back")) {
                break;
            }

            String[] info = input.split(" ");

            // Expecting 1 ID and 4 course scores
            if(info.length != 5) {
                System.out.println("Incorrect points format");
                continue;
            }

            try {
                int studentId = Integer.parseInt(info[0]);
                List<Integer> listOfStudents = student.getAllStudentsId();

                if(!listOfStudents.contains(studentId)) {
                    System.out.printf("No student is found for id=%d\n", studentId);
                    continue;
                }

                student.setId(studentId);
                boolean incorrectNumber = false;
                List<Integer> points = new ArrayList<>();

                // Parse the 4 course points
                for (int i = 1; i < info.length; i++) {
                    try {
                        int point = Integer.parseInt(info[i]);
                        if(point < 0) {
                            incorrectNumber = true;
                            break;
                        }
                        points.add(point);
                    } catch (NumberFormatException e) {
                        incorrectNumber = true;
                    }
                }

                if(incorrectNumber) {
                    System.out.println("Incorrect points format");
                } else {
                    student.updatePoints(points);
                    System.out.println("Points updated");
                }
            } catch (NumberFormatException e) {
                System.out.printf("No student is found for id=%s\n", info[0]);
            }
        }
    }

    /**
     * Finds and displays points for a specific student ID.
     */
    public static void findStudent() {
        System.out.println("Enter an id or 'back' to return");
        Student student = new Student();

        while (true) {
            String input = SCANNER.nextLine().trim();

            if (input.equals("back")) {
                break;
            }

            try {
                int studentid = Integer.parseInt(input);
                List<Integer> ids = student.getAllStudentsId();

                if(ids.contains(studentid)) {
                    student.setId(studentid);
                    // Fetch points string from the Student object
                    System.out.println(student.getPoints());
                } else {
                    System.out.printf("No student is found for id=%d.%n", studentid);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid ID or 'back'.");
            }
        }
    }
}

/**
 * Handles Student data, validation, and database operations.
 */
class Student {
    private int id;
    private String firstName;
    private String lastName;
    private String email;

    // Getters and Setters...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    /**
     * Validates names based on specific length and character requirements.
     */
    public static boolean validateName(String name) {
        if (name.length() < 2) {
            return false;
        }

        // RegEx: letters, with allowed hyphens or apostrophes in the middle, but not at ends.
        String regex = "^[a-zA-Z]+([-' ][a-zA-Z]+)*$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(name);

        return matcher.matches();
    }

    /**
     * Simple manual email validation.
     */
    public boolean validateEmail(String email) {
        if (email == null || email.isEmpty()) return false;

        // Check for exactly one '@'
        int atCount = 0;
        for (char c : email.toCharArray()) {
            if (c == '@') atCount++;
        }
        if (atCount != 1) return false;

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) return false;

        // Ensure there is a '.' after the '@'
        String afterAt = email.substring(atIndex + 1);
        int dotIndex = afterAt.indexOf('.');

        return dotIndex > 0 && dotIndex < afterAt.length() - 1;
    }

    /**
     * SQL to create the students table.
     */
    public void createTableStudents() {
        String sql = "CREATE TABLE IF NOT EXISTS students(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "firstname VARCHAR(100)," +
                "lastname VARCHAR(100), " +
                "email VARCHAR(100) UNIQUE" +
                ")";
        ConnectDBSqlite.createTable(sql);
    }

    public int getNumberStudents() {
        return getAllStudentsId().size();
    }

    /**
     * Fetches all unique student IDs from the database.
     */
    public List<Integer> getAllStudentsId() {
        String sql = "SELECT * FROM students ORDER BY id";
        List<Integer> list = new ArrayList<>();

        try(Connection connection = ConnectDBSqlite.getConnection();
            Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(sql);

            while (result.next()) {
                list.add(result.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Inserts a new student record into the DB.
     */
    public void addStudent() {
        String sql = "INSERT INTO students (id, firstName, lastName, email) VALUES (?, ?, ?, ?)";

        try(Connection connection = ConnectDBSqlite.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, getId());
            preparedStatement.setString(2, getFirstName());
            preparedStatement.setString(3, getLastName());
            preparedStatement.setString(4, getEmail());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if an email exists in the system to prevent duplicates.
     */
    public boolean isEmailUsed() {
        String sql = "SELECT * FROM students where email = ?";

        try(Connection connection = ConnectDBSqlite.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            ResultSet result = statement.executeQuery();
            return result.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Logic for updating points. Uses a manual "Upsert" (Update if exists, else Insert).
     */
    public void updatePoints(List<Integer> points) {
        String checkSql = "SELECT * FROM student_course WHERE student_id = ? AND course_id = ?";
        String updateSql = "UPDATE student_course SET points = points + ? WHERE student_id = ? AND course_id = ?";
        String insertSql = "INSERT INTO student_course (student_id, course_id, points) VALUES (?, ?, ?)";

        try(Connection connection = ConnectDBSqlite.getConnection()) {
            for(int courseId = 1; courseId <= points.size(); courseId++) {
                int pointValue = points.get(courseId - 1);

                try(PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, getId());
                    checkStmt.setInt(2, courseId);
                    ResultSet checkResult = checkStmt.executeQuery();

                    if(checkResult.next()) {
                        // If student already has a record for this course, add new points to existing
                        try(PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, pointValue);
                            updateStmt.setInt(2, getId());
                            updateStmt.setInt(3, courseId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Otherwise, create a new record for this course
                        try(PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, getId());
                            insertStmt.setInt(2, courseId);
                            insertStmt.setInt(3, pointValue);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all course points for the current student.
     * Uses COALESCE to ensure 0 is returned if no points record exists yet.
     */
    public String getPoints() {
        String sql = "SELECT c.name, COALESCE(sc.points, 0) as points " +
                "FROM courses c " +
                "LEFT JOIN student_course sc " +
                "ON c.id = sc.course_id AND sc.student_id = ? " +
                "ORDER BY c.id";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d points: ", getId()));

        try(Connection connection = ConnectDBSqlite.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, getId());

            ResultSet result = statement.executeQuery();
            boolean first = true;

            while (result.next()) {
                if(!first) sb.append("; ");
                String name = result.getString("name");
                int points = result.getInt("points");
                sb.append(String.format("%s=%d", name, points));
                first = false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}

/**
 * Handles course table creation and initialization.
 */
class Course {
    public void createTableCourses() {
        String sql = "CREATE TABLE IF NOT EXISTS courses(id INTEGER PRIMARY KEY, name VARCHAR(100) UNIQUE)";
        ConnectDBSqlite.createTable(sql);
    }

    public void createTableStudentCourse() {
        String sql = "CREATE TABLE IF NOT EXISTS student_course(" +
                "student_id INTEGER NOT NULL," +
                "course_id INTEGER NOT NULL, " +
                "points INTEGER DEFAULT 0," +
                "PRIMARY KEY (student_id, course_id)," +
                "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE," +
                "FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE" +
                ")";
        ConnectDBSqlite.createTable(sql);
    }

    /**
     * Populates the courses table with default names if they don't exist.
     */
    public void createCourses() {
        String sql = "INSERT OR IGNORE INTO courses (id, name) VALUES (?, ?)";
        String[][] courses = {
                {"1", "Java"}, {"2", "DSA"}, {"3", "Databases"}, {"4", "Spring"}
        };

        try (Connection connection = ConnectDBSqlite.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (String[] course : courses) {
                preparedStatement.setInt(1, Integer.parseInt(course[0]));
                preparedStatement.setString(2, course[1]);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

/**
 * Utility class for database connection management.
 */
class ConnectDBSqlite {
    private static Connection connection;
    private static String dbName = "db_sqlite";

    // Deletes the DB file to ensure a clean start for the tracker
    public static void deleteDB() {
        try {
            Files.deleteIfExists(Paths.get(dbName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Singleton-like connection getter
    public static Connection getConnection() throws SQLException {
        if(connection == null || connection.isClosed()) {
            String url = "jdbc:sqlite:" + dbName;
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl(url);
            connection = dataSource.getConnection();
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if(connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createTable(String sql) {
        try(Connection conn = getConnection();
            Statement statement = conn.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("Error creating table: " + e.getMessage());
            e.printStackTrace();
        }
    }
}