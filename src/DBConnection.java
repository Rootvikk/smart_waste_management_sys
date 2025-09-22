package src;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A utility class to manage the connection to the MySQL database.
 * This version creates a new connection on each request, which is a more robust
 * pattern for this type of application and avoids "connection closed" errors.
 */
public class DBConnection {

    // Database connection details
    // I've kept the '?serverTimezone=UTC' addition for compatibility with modern MySQL.
    private static final String URL = "jdbc:mysql://localhost:3306/waste_management?serverTimezone=UTC";
    private static final String USER = "root"; // <-- YOUR MYSQL USERNAME
    private static final String PASSWORD = "rootpass"; // <-- YOUR MYSQL PASSWORD

    // Private constructor to prevent instantiation
    private DBConnection() {}

    /**
     * Establishes and returns a new database connection.
     *
     * @return A new, active database connection.
     * @throws SQLException if a database access error occurs.
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Ensure the MySQL JDBC driver is loaded.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // This is a critical error, so we wrap it in a runtime exception.
            throw new RuntimeException("MySQL JDBC Driver not found!", e);
        }
        // This line now returns a fresh connection every time the method is called.
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}