package loglens.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// Centralized database utility for establishing connections and initializing schema.
public class DBConnection {

    // SQLite connection string pointing to local database file
    private static final String URL = "jdbc:sqlite:loglens.db";

    // Private constructor to prevent direct instantiation
    private DBConnection() {
    }

    // Creates and returns a new connection to the SQLite database
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // Initializes database schema if the tables do not already exist
    public static void createTables() {
        String sqlIncidents = "CREATE TABLE IF NOT EXISTS incidents ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "service TEXT NOT NULL, "
                + "pattern TEXT NOT NULL, "
                + "severity TEXT NOT NULL, "
                + "status TEXT NOT NULL, "
                + "opened_at TEXT NOT NULL, "
                + "acknowledged_at TEXT, "
                + "resolved_at TEXT, "
                + "handled_by TEXT"
                + ");";

        String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT UNIQUE NOT NULL, "
                + "password_hash TEXT NOT NULL, "
                + "role TEXT NOT NULL"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlIncidents);
            stmt.execute(sqlUsers);
        } catch (SQLException e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
        }
    }
}
