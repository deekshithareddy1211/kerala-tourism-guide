package main.java.org.example;

import spark.Response;
import static spark.Spark.*;
import java.io.*;
import java.sql.*;

public class KeralaTourism {

    private static final String URL = "jdbc:sqlite:sample.db";
    private static final String STATIC_FILES_DIR = "src/main/resources/";

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");

        // Initialize the database
        initializeDatabase();

        // Set static file location for serving CSS, images, and JS
        staticFiles.externalLocation(STATIC_FILES_DIR);

        // Serve the homepage (homepage.html)
        get("/", (_, res) -> serveFile("homepage.html", res));

        // Serve the explore page for 'Explore Now' button
        get("/explore", (_, res) -> serveFile("explore.html", res));

        // Serve the Plan Your Visit page
        get("/plan_your_visit", (_, res) -> serveFile("plan_your_visit.html", res));

        // Signup route
        post("/signup", (req, res) -> {
            String email = req.queryParams("email");
            String password = req.queryParams("password");
            try {
                signUpUser(email, password);
                res.status(201);
                return "User signed up successfully.";
            } catch (SQLException e) {
                res.status(400);
                return "Error signing up: " + e.getMessage();
            }
        });

        // Login route
        post("/login", (req, res) -> {
            String email = req.queryParams("email");
            String password = req.queryParams("password");
            boolean success = loginUser(email, password);
            if (success) {
                res.cookie("userEmail", email);
                res.status(200);
                return "Login successful";
            } else {
                res.status(401);
                return "Login failed: Incorrect email or password.";
            }
        });

        // Plan visit route (handling form submission)
        post("/submitVisit", (req, res) -> {
            String name = req.queryParams("name");
            String email = req.queryParams("email");
            String phone = req.queryParams("phone");
            String visitDate = req.queryParams("visit_date");
            String message = req.queryParams("message");
            try {
                planVisit(name, email, phone, visitDate, message);
                res.status(201);
                return "Visit planned successfully!";
            } catch (SQLException e) {
                res.status(400);
                return "Error planning visit: " + e.getMessage();
            }
        });

        System.out.println("Server Running on http://127.0.0.1:4567");
    }

    // Initialize Database
    private static void initializeDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            // Visit Plans table
            String createVisitPlansTable = "CREATE TABLE IF NOT EXISTS visit_plans (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_name TEXT, " +
                    "user_email TEXT, " +
                    "user_phone TEXT, " +
                    "visit_date TEXT, " +
                    "message TEXT);";
            stmt.execute(createVisitPlansTable);

            System.out.println("Database initialized successfully.");
        }
    }

    // User sign-up
    public static void signUpUser(String email, String password) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (email, password) VALUES (?, ?)")) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);  // Consider hashing the password in real applications
            pstmt.executeUpdate();
        }
    }

    // User login
    public static boolean loginUser(String email, String password) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT password FROM users WHERE email = ?")) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getString("password").equals(password);  // Compare hashed passwords in real applications
        }
    }

    // Plan visit
    private static void planVisit(String name, String email, String phone, String visitDate, String message) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO visit_plans (user_name, user_email, user_phone, visit_date, message) VALUES (?, ?, ?, ?, ?)")) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, phone);
            pstmt.setString(4, visitDate);
            pstmt.setString(5, message);
            pstmt.executeUpdate();
        }
    }

    // Serve HTML files
    private static boolean serveFile(String filePath, Response res) throws IOException {
        File file = new File(STATIC_FILES_DIR + filePath);
        if (!file.exists() || file.isDirectory()) {
            halt(404, "File not found");
            return false;
        }
        res.type(determineContentType(filePath));

        try (InputStream in = new FileInputStream(file);
             OutputStream out = res.raw().getOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            out.flush();
        }
        return true;
    }

    // Determine Content Type
    private static String determineContentType(String filePath) {
        if (filePath.endsWith(".css")) return "text/css";
        else if (filePath.endsWith(".js")) return "application/javascript";
        else if (filePath.endsWith(".html")) return "text/html";
        else if (filePath.endsWith(".png")) return "image/png";
        else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
        else return "application/octet-stream";
    }
}
