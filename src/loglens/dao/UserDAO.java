package loglens.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import loglens.model.User;
import loglens.util.PasswordUtil;

// Data Access Object (DAO) for managing users and handling authentication against SQLite.
//
// Why does authentication happen in the DAO?
// In layered architecture, the DAO layer is exclusively responsible for database queries, connection
// management, and SQL operations. Authentication requires verifying user credentials stored in the
// database. Placing authentication inside UserDAO keeps UI and business logic clean and decoupled
// from SQL details, and ensures queries are parameterized with PreparedStatements to prevent SQL injection.
public class UserDAO {

    // Seeds default accounts (admin and viewer) if the users table is currently empty
    public void seedDefaultUsers() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM users;";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?);";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    // Seed admin account
                    insertStmt.setString(1, "admin");
                    insertStmt.setString(2, PasswordUtil.hash("admin123"));
                    insertStmt.setString(3, User.ROLE_ADMIN);
                    insertStmt.executeUpdate();

                    // Seed viewer account
                    insertStmt.setString(1, "viewer");
                    insertStmt.setString(2, PasswordUtil.hash("viewer123"));
                    insertStmt.setString(3, User.ROLE_VIEWER);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    // Authenticates a user by username and plaintext password; returns User on match, or null on failure
    public User authenticate(String username, String password) throws SQLException {
        if (username == null || password == null) {
            return null;
        }

        String sql = "SELECT id, username, password_hash, role FROM users WHERE username = ?;";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String computedHash = PasswordUtil.hash(password);

                    if (storedHash.equals(computedHash)) {
                        return new User(rs.getInt("id"), rs.getString("username"), rs.getString("role"));
                    }
                }
            }
        }

        return null;
    }
}
