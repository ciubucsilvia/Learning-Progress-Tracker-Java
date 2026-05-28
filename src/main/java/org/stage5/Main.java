package org.stage5;

import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main entry point for the Learning Progress Tracker.
 * Manages the CLI loop and delegates commands to specific methods.
 */
public class Main {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Integer firstId = 10000; // Starting ID for students
    private static boolean inStatisticsMode = false;

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
                case "back" -> back();
                case "add students" -> addStudents();
                case "list" -> getList();
                case "add points" -> addPoints();
                case "find" -> findStudent();
                case "statistics" -> {
                    inStatisticsMode = true;
                    getStatistics();
                }
                case "notify" -> notifications();
                default -> {
                    if(inStatisticsMode) {
                        getInfoAboutSelectedCourse(input);
                    } else {
                        System.out.println("Unknown command!");
                    }
                }
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

        student.createTableNotifications();

        // Populate the courses table with default values
        course.createCourses();
    }

    /**
     * Handles the 'back' command at the top level or shows student count.
     */
    public static void back() {
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
                    System.out.println("Points updated.");
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
                int studentId = Integer.parseInt(input);
                List<Integer> ids = student.getAllStudentsId();

                if(ids.contains(studentId)) {
                    student.setId(studentId);
                    // Fetch points string from the Student object
                    System.out.println(student.getPoints());
                } else {
                    System.out.printf("No student is found for id=%d.%n", studentId);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid ID or 'back'.");
            }
        }
    }

    public static void getStatistics() {
        Course course = new Course();

        String no = "n/a";

        String mostPopular = course.getMostPopular();
        String leastPopular = course.getLeastPopular();
        String highestActivity = course.getHighestActivity();
        String lowestActivity = course.getLowestActivity();
        String easiestCourse = course.getEasiestCourse();
        String hardestCourse = course.getHardestCourse();

        System.out.println("Type the name of a course to see details or 'back' to quit:");
        System.out.println("Most popular: " +
                (mostPopular.isEmpty() ? no : mostPopular));
        System.out.println("Least popular: " +
                (leastPopular.isEmpty() ? no : leastPopular));
        System.out.println("Highest activity: " +
                (highestActivity.isEmpty() ? no : highestActivity));
        System.out.println("Lowest activity: " +
                (lowestActivity.isEmpty() ? no : lowestActivity));
        System.out.println("Easiest course: " +
                (easiestCourse.isEmpty() ? no : easiestCourse));
        System.out.println("Hardest course: " +
                (hardestCourse.isEmpty() ? no : hardestCourse));

//        getInfoAboutSelectedCourse(course);
    }

    public static void getInfoAboutSelectedCourse(String courseName) {
        Course course = new Course();

        if(courseName.equals("back")) {
            inStatisticsMode = false;
            return;
        }

        String nameFromDb = course.ifExists(courseName);

        if(nameFromDb.isEmpty()) {
            System.out.println("Unknown course.");
            return;
        }

        // Get course details
        course.getCourseDetails(nameFromDb);
    }

    public static void notifications() {
        Course course = new Course();

        Map<Integer, Set<Integer>> listNotifiedStudents= course.getListNotifiedStudents();

        List<NotifyInfo> listFinisedCourses = course.getListFinisedCourses();

        int notifiedStudent = 0;
        int countNotifiedStudents = 0;

        for(NotifyInfo info: listFinisedCourses) {
            int studentId = info.student.getId();
            int courseId = info.course.getId();

            // Check if already notified for this specific course
            if(listNotifiedStudents.containsKey(studentId) &&
                    (listNotifiedStudents.get(studentId).contains(courseId))) {
                continue;
            }

            if(notifiedStudent != studentId) {
                notifiedStudent = studentId;
                countNotifiedStudents++;
            }

            // Update notification in database
            course.updateNotifications(studentId, courseId);

            System.out.printf("To: %s\n", info.student.getEmail());
            System.out.println("Re: Your Learning Progress");
            System.out.printf("Hello, %s! " +
                            "You have accomplished our %s course!\n",
                    info.student.getFirstName() + " " + info.student.getLastName(),
                    info.course.getName());
        }

        System.out.printf("Total %d students have been notified\n", countNotifiedStudents);
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

    public void createTableNotifications() {
        String sql = "CREATE TABLE IF NOT EXISTS notifications(" +
                "student_id INTEGER NOT NULL, " +
                "course_id INTEGER NOT NULL, " +
                "FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE" +
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
        String checkSql = "SELECT * FROM student_course " +
                "WHERE student_id = ? AND course_id = ?";

        String updateSql = "UPDATE student_course " +
                "SET points = points + ?, submissions = submissions + ? " +
                "WHERE student_id = ? AND course_id = ?";

        String insertSql = "INSERT INTO student_course " +
                "(student_id, course_id, points, submissions) " +
                "VALUES (?, ?, ?, ?)";

        try(Connection connection = ConnectDBSqlite.getConnection()) {
            for(int courseId = 1; courseId <= points.size(); courseId++) {
                int pointValue = points.get(courseId - 1);
                int submissionIncrement = pointValue > 0 ? 1 : 0;

                try(PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, getId());
                    checkStmt.setInt(2, courseId);
                    ResultSet checkResult = checkStmt.executeQuery();

                    if(checkResult.next()) {
                        // If student already has a record for this course, add new points to existing
                        try(PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, pointValue);
                            updateStmt.setInt(2, submissionIncrement);
                            updateStmt.setInt(3, getId());
                            updateStmt.setInt(4, courseId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Otherwise, create a new record for this course
                        try(PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, getId());
                            insertStmt.setInt(2, courseId);
                            insertStmt.setInt(3, pointValue);
                            insertStmt.setInt(4, submissionIncrement);
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
    private int id;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Course completion requirements
    private static final Map<String, Integer> COURSE_REQUIREMENTS = Map.of(
            "Java", 600,
            "DSA", 400,
            "Databases", 480,
            "Spring", 550
    );


    public void createTableCourses() {
        String sql = "CREATE TABLE IF NOT EXISTS courses(id INTEGER PRIMARY KEY, name VARCHAR(100) UNIQUE)";
        ConnectDBSqlite.createTable(sql);
    }

    public void createTableStudentCourse() {
        String sql = "CREATE TABLE IF NOT EXISTS student_course(" +
                "student_id INTEGER NOT NULL," +
                "course_id INTEGER NOT NULL, " +
                "points INTEGER DEFAULT 0," +
                "submissions INTEGER DEFAULT 0," +
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

    private boolean hasAnyStudent() {
        String sql = "SELECT COUNT(*) as count FROM students";
        try (Connection connection = ConnectDBSqlite.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(sql);
            if (result.next()) {
                return result.getInt("count") > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getMostPopular() {
        if (!hasAnyStudent()) {
            return "";
        }

        String sql = "SELECT courses.name as name, " +
                "COUNT(DISTINCT student_course.student_id) as student_count FROM courses " +
                "LEFT JOIN student_course " +
                "ON student_course.course_id = courses.id " +
                "WHERE points > 0 " +
                "GROUP BY courses.id ORDER BY course_id DESC";

        return getCategoryWithMultiple(sql, "student_count", true);
    }

    public String getLeastPopular() {
        if (!hasAnyStudent()) {
            return "";
        }

        String mostPopular = getMostPopular();
        String sql = "SELECT courses.name as name, " +
                "COUNT(DISTINCT student_course.student_id) as student_count " +
                "FROM courses " +
                "JOIN student_course " +
                "ON student_course.course_id = courses.id AND points > 0 " +
                "GROUP BY courses.id " +
                "ORDER BY student_count ASC";

        String result = getCategoryWithMultiple(sql, "student_count", false);

        if (result.isEmpty()) {
            return "";
        }

        if (mostPopular.isEmpty()) {
            return result;
        }

        // Remove courses that are in most popular
        if(!mostPopular.isEmpty() && !result.isEmpty()) {
            List<String> mostPopularList = Arrays.asList(mostPopular.split(", "));
            List<String> resultList = new ArrayList<>(Arrays.asList(result.split(", ")));
            resultList.removeAll(mostPopularList);

            return String.join(", ", resultList);
        }


        if (result.isEmpty() && !mostPopular.isEmpty()) {
            return getCategoryWithMultiple(sql, "student_count", false);
        }

        return result;
    }

    public String getHighestActivity() {
        if (!hasAnyStudent()) {
            return "";
        }

        String sql = "SELECT courses.name as name, " +
                "COALESCE(SUM(student_course.submissions), 0) as activity_count FROM courses " +
                "LEFT JOIN student_course " +
                "ON student_course.course_id = courses.id " +
                "GROUP BY courses.id ";

        return getCategoryWithMultiple(sql, "activity_count", true);
    }

    public String getLowestActivity() {
        if (!hasAnyStudent()) {
            return "";
        }

        String highestActivity = getHighestActivity();
        String sql = "SELECT courses.name as name, " +
                "COALESCE(SUM(student_course.submissions), 0) as activity_count FROM courses " +
                "LEFT JOIN student_course " +
                "ON student_course.course_id = courses.id " +
                "GROUP BY courses.id ";

        String result = getCategoryWithMultiple(sql, "activity_count", false);

        // Remove courses that are in highest activity
        if (!highestActivity.isEmpty() && !result.isEmpty()) {
            List<String> highestList = Arrays.asList(highestActivity.split(", "));
            List<String> resultList = new ArrayList<>(Arrays.asList(result.split(", ")));
            resultList.removeAll(highestList);
            return String.join(", ", resultList);
        }

        if (result.isEmpty() && !highestActivity.isEmpty()) {
            return getCategoryWithMultiple(sql, "points", false);
        }

        return result;
    }

    public String getEasiestCourse() {
        if (!hasAnyStudent()) {
            return "";
        }

        String sql = "SELECT courses.name as name, " +
                "AVG(student_course.points) as points FROM courses " +
                "JOIN student_course " +
                "ON student_course.course_id = courses.id " +
                "GROUP BY courses.id " +
                "HAVING COUNT(student_course.student_id) > 0 " +
                "ORDER BY points DESC";

        return getCategoryWithMultiple(sql, "points", true);
    }

    public String getHardestCourse() {
        if (!hasAnyStudent()) {
            return "";
        }

        String easiestCourse = getEasiestCourse();
        String sql = "SELECT courses.name as name, " +
                "AVG(student_course.points) as points FROM courses " +
                "JOIN student_course " +
                "ON student_course.course_id = courses.id  AND points > 0 " +
                "GROUP BY courses.id " +
                "HAVING COUNT(student_course.student_id) > 0 ";

        String result = getCategoryWithMultiple(sql, "points", false);

        // Remove courses that are in easiest course
        if (!easiestCourse.isEmpty() && !result.isEmpty()) {
            List<String> easiestList = Arrays.asList(easiestCourse.split(", "));
            List<String> resultList = new ArrayList<>(Arrays.asList(result.split(", ")));
            resultList.removeAll(easiestList);
            return String.join(", ", resultList);
        }
        return result;
    }

    private String getCategoryWithMultiple(String sql, String orderColumn, boolean highest) {
        Map<String, Double> courseValues = new LinkedHashMap<>();

        try (Connection connection = ConnectDBSqlite.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(sql);

            while (result.next()) {
                String name = result.getString("name");
                double value = result.getDouble(orderColumn);
                courseValues.put(name, value);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (courseValues.isEmpty()) {
            return "";
        }

        // Găsește valoarea extremă
        double extremeValue = highest ? Double.MIN_VALUE : Double.MAX_VALUE;
        for (double value : courseValues.values()) {
            if (highest && value > extremeValue) {
                extremeValue = value;
            } else if (!highest && value < extremeValue) {
                extremeValue = value;
            }
        }

        // Colectează toate cursurile cu valoarea extremă
        List<String> extremeCourses = new ArrayList<>();
        for (Map.Entry<String, Double> entry : courseValues.entrySet()) {
            // Folosește >= sau <= în loc de > sau < pentru a include toate valorile egale
            if (highest) {
                if (entry.getValue() >= extremeValue) {
                    extremeCourses.add(entry.getKey());
                }
            } else {
                if (entry.getValue() <= extremeValue) {
                    extremeCourses.add(entry.getKey());
                }
            }
        }

        Collections.sort(extremeCourses);
        return String.join(", ", extremeCourses);
    }

    public String ifExists(String course) {
        String sql = "SELECT * FROM courses WHERE UPPER(name) = UPPER(?)";

        try(Connection connection = ConnectDBSqlite.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, course);
            ResultSet result = preparedStatement.executeQuery();

            if(result.next()) {
                return result.getString("name");
            }

        } catch (SQLException e) {
            return "";
        }
        return "";
    }

    public void getCourseDetails(String course) {
        System.out.println(course);
        System.out.println("id    points    completed");

        // Get the course ID
        int courseId = getCourseId(course);

        if(courseId == -1) {
            return;
        }

        // Get students with points for this course
        String sql = "SELECT student_id, points FROM student_course " +
                "WHERE course_id = ? AND points > 0 " +
                "ORDER BY points DESC, student_id ASC";

        List<StudentInfo> students = new ArrayList<>();
        int requirement = COURSE_REQUIREMENTS.get(course);

        try(Connection connection = ConnectDBSqlite.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, courseId);
            ResultSet result = preparedStatement.executeQuery();

            while (result.next()) {
                int studentId = result.getInt("student_id");
                int points = result.getInt("points");
                double completed = (double) points / requirement * 100;
                students.add(new StudentInfo(studentId, points, completed));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Display students
        for (StudentInfo student : students) {
            System.out.printf("%d %d      %.1f%%\n",
                    student.id, student.points, student.completed);
        }
    }

    private int getCourseId(String courseName) {
        String sql = "SELECT id FROM courses WHERE name = ?";
        try (Connection connection = ConnectDBSqlite.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, courseName);
            ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                return result.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public Map<Integer, Set<Integer>> getListNotifiedStudents() {
        Map<Integer, Set<Integer>> listNotifiedStudents= new HashMap<>();

        String sql = "SELECT * FROM notifications";

        try(Connection connection = ConnectDBSqlite.getConnection();
            Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(sql);

            while (result.next()) {
                Integer student_id = result.getInt("student_id");
                Integer course_id = result.getInt("course_id");

                listNotifiedStudents
                        .computeIfAbsent(student_id, k -> new HashSet<>())
                        .add(course_id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return listNotifiedStudents;
    }

    public List getListFinisedCourses() {
        List<NotifyInfo> list = new ArrayList<>();

        String sql = "SELECT students.id as studentId," +
                "students.firstname as firstname," +
                "students.lastname as lastname," +
                "students.email as email, " +
                "courses.id as courseId, " +
                "courses.name as courseName, " +
                "SUM(student_course.points) as points " +
                "FROM student_course " +
                "JOIN students ON student_course.student_id = students.id " +
                "JOIN courses ON student_course.course_id = courses.id " +
                "GROUP BY students.id, courses.id";

        try(Connection connection = ConnectDBSqlite.getConnection();
            Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(sql);

            while (result.next()) {
                String courseName = result.getString("courseName");
                Integer points = result.getInt("points");

                int requirement = COURSE_REQUIREMENTS.get(courseName);

                if(points >= requirement) {
                    Course course = new Course();
                    course.setName(courseName);
                    course.setId(result.getInt("courseId"));

                    Student student = new Student();
                    student.setId(result.getInt("studentId"));
                    student.setFirstName(result.getString("firstname"));
                    student.setLastName(result.getString("lastname"));
                    student.setEmail(result.getString("email"));

                    list.add(new NotifyInfo(student, course));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public void updateNotifications(int studentId, int courseId) {
        String sql = "INSERT INTO notifications " +
                "(student_id, course_id) VALUES (?, ?)";

        try(Connection connection = ConnectDBSqlite.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, courseId);
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static class StudentInfo {
        int id;
        int points;
        double completed;

        StudentInfo(int id, int points, double completed) {
            this.id = id;
            this.points = points;
            this.completed = completed;
        }
    }
}

class NotifyInfo {
    Student student;
    Course course;

    NotifyInfo(Student student, Course course) {
        this.student = student;
        this.course = course;
    }

}

/**
 * Utility class for database connection management.
 */
class ConnectDBSqlite {
    private static Connection connection;
    private static final String dbName = "db_sqlite.db";

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