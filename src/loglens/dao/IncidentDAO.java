package loglens.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import loglens.model.Incident;

// Data Access Object (DAO) for Incident entities.
//
// What is a DAO?
// The DAO pattern encapsulates all database CRUD (Create, Read, Update, Delete) operations.
// By keeping SQL queries in this dedicated class, the rest of the application remains clean,
// maintainable, and independent of specific database technology.
//
// Why use PreparedStatement instead of string concatenation?
// 1. Security: Concatenating input into SQL strings opens the door to SQL Injection attacks.
//    PreparedStatement sends parameters separately from the SQL command, neutralizing malicious input.
// 2. Performance: PreparedStatement allows the database to pre-compile the SQL query plan,
//    making repeated executions significantly faster.
public class IncidentDAO {

    // Formatter used to store and read dates as TEXT in SQLite: "yyyy-MM-dd HH:mm:ss"
    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Inserts a new incident into the database and sets the auto-generated id on the object
    public int insert(Incident incident) throws SQLException {
        String sql = "INSERT INTO incidents (service, pattern, severity, status, opened_at, acknowledged_at, resolved_at, handled_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, incident.getService());
            stmt.setString(2, incident.getPattern());
            stmt.setString(3, incident.getSeverity());
            stmt.setString(4, incident.getStatus());
            stmt.setString(5, formatDateTime(incident.getOpenedAt()));
            stmt.setString(6, formatDateTime(incident.getAcknowledgedAt()));
            stmt.setString(7, formatDateTime(incident.getResolvedAt()));
            stmt.setString(8, incident.getHandledBy());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    incident.setId(id);
                    return id;
                } else {
                    throw new SQLException("Creating incident failed, no ID obtained.");
                }
            }
        }
    }

    // Retrieves all incidents sorted newest first
    public List<Incident> findAll() throws SQLException {
        String sql = "SELECT * FROM incidents ORDER BY id DESC";
        List<Incident> list = new ArrayList<Incident>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // Retrieves incidents filtered by their current status
    public List<Incident> findByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM incidents WHERE status = ? ORDER BY id DESC";
        List<Incident> list = new ArrayList<Incident>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    // Retrieves an incident by its primary key ID, or null if not found
    public Incident findById(int id) throws SQLException {
        String sql = "SELECT * FROM incidents WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    // Transitions an incident from OPEN to ACKNOWLEDGED.
    //
    // Why check status rules directly in the SQL WHERE clause?
    // Checking "WHERE id = ? AND status = ?" enforces the business transition at the database level.
    // In concurrent environments, this prevents race conditions: if another thread already modified
    // the status, executeUpdate() will return 0 rows changed, ensuring safe atomic updates.
    public boolean acknowledge(int id, String handledBy) throws SQLException {
        String sql = "UPDATE incidents SET status = ?, acknowledged_at = ?, handled_by = ? "
                + "WHERE id = ? AND status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, Incident.STATUS_ACKNOWLEDGED);
            stmt.setString(2, formatDateTime(LocalDateTime.now()));
            stmt.setString(3, handledBy);
            stmt.setInt(4, id);
            stmt.setString(5, Incident.STATUS_OPEN);

            return stmt.executeUpdate() > 0;
        }
    }

    // Transitions an incident from ACKNOWLEDGED to RESOLVED.
    // Only succeeds if the incident is currently in ACKNOWLEDGED status.
    public boolean resolve(int id) throws SQLException {
        String sql = "UPDATE incidents SET status = ?, resolved_at = ? "
                + "WHERE id = ? AND status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, Incident.STATUS_RESOLVED);
            stmt.setString(2, formatDateTime(LocalDateTime.now()));
            stmt.setInt(3, id);
            stmt.setString(4, Incident.STATUS_ACKNOWLEDGED);

            return stmt.executeUpdate() > 0;
        }
    }

    // Deletes an incident record by ID
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM incidents WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // Converts a LocalDateTime to a formatted string, or null
    private String formatDateTime(LocalDateTime dateTime) {
        return (dateTime != null) ? dateTime.format(FORMATTER) : null;
    }

    // Converts a formatted string to LocalDateTime, or null
    private LocalDateTime parseDateTime(String text) {
        return (text != null && !text.trim().isEmpty()) ? LocalDateTime.parse(text, FORMATTER) : null;
    }

    // Maps a ResultSet row into a domain Incident object
    private Incident mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String service = rs.getString("service");
        String pattern = rs.getString("pattern");
        String severity = rs.getString("severity");
        String status = rs.getString("status");
        LocalDateTime openedAt = parseDateTime(rs.getString("opened_at"));
        LocalDateTime acknowledgedAt = parseDateTime(rs.getString("acknowledged_at"));
        LocalDateTime resolvedAt = parseDateTime(rs.getString("resolved_at"));
        String handledBy = rs.getString("handled_by");

        return new Incident(id, service, pattern, severity, status, 
                openedAt, acknowledgedAt, resolvedAt, handledBy);
    }
}
